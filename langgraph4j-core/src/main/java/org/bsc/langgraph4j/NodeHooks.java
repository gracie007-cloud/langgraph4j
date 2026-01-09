package org.bsc.langgraph4j;

import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.hook.NodeHook;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.AgentStateFactory;
import org.bsc.langgraph4j.state.Channel;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.bsc.langgraph4j.utils.CollectionsUtils.mergeMap;

class NodeHooks<State extends AgentState> {

    private record WrapCallHolder<State extends AgentState>  (
            NodeHook.WrapCall<State> delegate,
            AsyncNodeActionWithConfig<State> action
    )  implements AsyncNodeActionWithConfig<State> {

        @Override
        public CompletableFuture<Map<String, Object>> apply(State state, RunnableConfig config) {
            return delegate.apply(state, config, action);
        }
    }

    // BEFORE CALL HOOK
    private Deque<NodeHook.BeforeCall<State>> beforeCallList;

    private Stream<NodeHook.BeforeCall<State>> beforeCallListAsStream( ) {
        return ofNullable(beforeCallList).stream().flatMap(Collection::stream);
    }

    public void addBeforeCall(NodeHook.BeforeCall<State> beforeCall ) {
        requireNonNull( beforeCall, "beforeCall cannot be null");

        if( beforeCallList == null ) { // Lazy Initialization
            beforeCallList = new ArrayDeque<>();
        }

        beforeCallList.push(beforeCall);
    }


    public CompletableFuture<Map<String, Object>> applyBeforeCall(State state, RunnableConfig config, AgentStateFactory<State> stateFactory, Map<String, Channel<?>> schema ) {
        return beforeCallListAsStream()
                .reduce( completedFuture(state.data()),
                        (futureResult, call) ->
                                futureResult.thenCompose( result -> call.accept(stateFactory.apply(result), config)
                                        .thenApply( partial -> AgentState.updateState( result, partial, schema ) )),
                        (f1, f2) -> f1.thenCompose(v -> f2) // Combiner for parallel streams
                );
    }

    // AFTER CALL HOOK
    private Deque<NodeHook.AfterCall<State>> afterCallList;

    private Stream<NodeHook.AfterCall<State>> afterCallListAsStream( ) {
        return ofNullable(afterCallList).stream().flatMap(Collection::stream);
    }

    public void addAfterCall(NodeHook.AfterCall<State> afterCall ) {
        requireNonNull( afterCall, "afterCall cannot be null");

        if( afterCallList == null ) { // Lazy Initialization
            afterCallList = new ArrayDeque<>();
        }

        afterCallList.push(afterCall);
    }

    public CompletableFuture<Map<String, Object>> applyAfterCall(State state, RunnableConfig config, Map<String,Object> partialResult ) {
        return afterCallListAsStream()
                .reduce( completedFuture(partialResult),
                        (futureResult, call) ->
                                futureResult.thenCompose( result -> call.accept( state, config, result)
                                        .thenApply( partial -> mergeMap(result, partial, ( oldValue, newValue) -> newValue ) )),
                        (f1, f2) -> f1.thenCompose(v -> f2) // Combiner for parallel streams
                );
    }

    // WRAP CALL HOOK
    private Map<String,Deque<NodeHook.WrapCall<State>>> wrapCallMap;
    private Deque<NodeHook.WrapCall<State>> wrapCallList;


    public void addWrapCall(NodeHook.WrapCall<State> wrapCall ) {
        requireNonNull( wrapCall, "wrapCall cannot be null");

        if( wrapCallList == null ) { // Lazy Initialization
            wrapCallList = new ArrayDeque<>();
        }

        wrapCallList.push(wrapCall);
    }

    public void addWrapCall(String nodeId, NodeHook.WrapCall<State> wrapCall ) {
        requireNonNull( nodeId, "nodeId cannot be null");
        requireNonNull( wrapCall, "wrapCall cannot be null");

        if( wrapCallMap == null ) { // Lazy Initialization
            wrapCallMap = new HashMap<>();
        }

        wrapCallMap.computeIfAbsent(nodeId, k -> new ArrayDeque<>())
                    .push(wrapCall);

    }

    private Stream<NodeHook.WrapCall<State>> wrapCallListAsStream( ) {
        return ofNullable(wrapCallList).stream().flatMap(Collection::stream);
    }

    private Stream<NodeHook.WrapCall<State>> wrapCallMapAsStream(String nodeId ) {
        return ofNullable(wrapCallMap).stream()
                .flatMap( map ->
                    ofNullable( map.get(nodeId) ).stream()
                            .flatMap( Collection::stream ));
    }

    public CompletableFuture<Map<String, Object>> applyWrapCall( AsyncNodeActionWithConfig<State> action, State state, RunnableConfig config, Map<String, Channel<?>> schema ) {
        return Stream.concat( wrapCallListAsStream(), wrapCallMapAsStream(config.nodeId()))
                .reduce(action,
                        (acc, wrapper) -> new WrapCallHolder<>(wrapper, acc),
                        (v1, v2) -> v1)
                .apply(state, config);
    }

}
