package org.bsc.langgraph4j.hook;

import org.bsc.langgraph4j.LG4JLoggable;
import org.bsc.langgraph4j.action.Command;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;

import java.util.List;
import java.util.Map;

import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.bsc.langgraph4j.utils.CollectionsUtils.mergeMap;

// , Map<String, Channel<?>> schema
public record NestedEdgeHook<State extends AgentState>(String level)
        implements  LG4JLoggable {

    public static final String AFTER_CALL_ATTRIBUTE = "edge-after-call";
    public static final String HOOKS_ATTRIBUTE = "edge-hooks";

    public static <State extends AgentState> NestedEdgeHook<State> of( String level ) {
        return new NestedEdgeHook<>(level);
    }

    public EdgeHook.AfterCall<State> applyAfterHook( Map<String, Channel<?>> schema ) {
        return (sourceId, state, config, lastResult) -> {
            log.info("edge after call start: hook on '{}' level '{}'", sourceId, level);
            return completedFuture(
                    new Command( lastResult.gotoNodeSafe().orElse(null),
                            AgentState.updateState( lastResult.update(), Map.of(AFTER_CALL_ATTRIBUTE, 1), schema )))
                    .whenComplete( ( result, exception ) ->
                            log.info("edge after call end: hook on '{}' level '{}'", sourceId, level));

        };
    }

    public EdgeHook.BeforeCall<State> applyBeforeHook() {
        return (sourceId, state,  config) -> {
            log.info("edge before call start: hook on '{}' level '{}'", sourceId, level);

            return completedFuture( new Command( Map.of(HOOKS_ATTRIBUTE, Map.of( sourceId, List.of(level)))))
                    .whenComplete( ( result, exception ) ->
                            log.info("edge before call end: hook on '{}' level '{}'", sourceId, level));

        };
    }

    public EdgeHook.WrapCall<State> applyWrapHook() {
        return ( sourceId, state, config,  action) -> {
            log.info("edge wrap call start: hook on '{}' level '{}'", sourceId, level);
            return action.apply(state, config)
                    .thenApply(result ->
                            new Command( result.gotoNodeSafe().orElse(null),
                                    mergeMap( result.update(),
                                            Map.of(HOOKS_ATTRIBUTE, Map.of( sourceId, List.of(level))),
                                            ( left, right) -> right )) )
                    .whenComplete( ( result, exception ) -> {
                        log.info("edge wrap call end: hook on '{}' level '{}'", sourceId, level);
                    });

        };
    }
}
