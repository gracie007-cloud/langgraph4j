
# Graph Hooks

Hooks in LangGraph4j provide a powerful mechanism to intercept the execution of graph nodes and edges. They allow you to run custom code before, after, or around the core logic of a node or a conditional edge. This is particularly useful for tasks such as:

*   Logging and tracing execution flow.
*   Collecting metrics and performance data.
*   Modifying the state between steps.
*   Implementing custom debugging tools.
*   Altering the control flow of the graph dynamically.

## Hook Interfaces

There are two main interfaces for hooks:

*   `org.bsc.langgraph4j.hook.NodeHook`: For instrumenting graph nodes.
*   `org.bsc.langgraph4j.hook.EdgeHook`: For instrumenting conditional edges (since only they have an associated function).

## Hook Types

Both `NodeHook` and `EdgeHook` support three distinct features, allowing for fine-grained control over the execution interception:

*   **`BeforeCall`**: This hook is executed *before* the node or edge function is called. It receives the current `AgentState` and `RunnableConfig`.
*   **`AfterCall`**: This hook is executed *after* the node or edge function completes. It receives the `AgentState`, `RunnableConfig`, and the result of the function call.
*   **`WrapCall`**: This hook "wraps" the execution of the node or edge function. It allows you to execute code both before and after the function call, and even to decide whether to call the original function at all. It receives the `AgentState`, `RunnableConfig` and the action to execute.

## Registering Hooks

You can add hooks to a `StateGraph` instance in two ways:

*   **Global Hooks**: These hooks apply to *all* nodes or conditional edges in the graph.
*   **ID-Specific Hooks**: These hooks apply only to a *specific* node or conditional edge, identified by its unique ID.

You can register multiple hooks, both global and specific.

Here are the methods available on the `StateGraph` class for adding hooks:

```java
// Node Hooks
StateGraph<State> addBeforeCallNodeHook(NodeHook.BeforeCall<State> hook)
StateGraph<State> addBeforeCallNodeHook(String nodeId, NodeHook.BeforeCall<State> hook)
StateGraph<State> addAfterCallNodeHook(NodeHook.AfterCall<State> hook)
StateGraph<State> addAfterCallNodeHook(String nodeId, NodeHook.AfterCall<State> hook)
StateGraph<State> addWrapCallNodeHook(NodeHook.WrapCall<State> hook)
StateGraph<State> addWrapCallNodeHook(String nodeId, NodeHook.WrapCall<State> hook)

// Edge Hooks
StateGraph<State> addBeforeCallEdgeHook(EdgeHook.BeforeCall<State> hook)
StateGraph<State> addBeforeCallEdgeHook(String edgeId, EdgeHook.BeforeCall<State> hook)
StateGraph<State> addAfterCallEdgeHook(EdgeHook.AfterCall<State> hook)
StateGraph<State> addAfterCallEdgeHook(String edgeId, EdgeHook.AfterCall<State> hook)
StateGraph<State> addWrapCallEdgeHook(EdgeHook.WrapCall<State> hook)
StateGraph<State> addWrapCallEdgeHook(String edgeId, EdgeHook.WrapCall<State> hook)
```

## Execution Order

When multiple hooks are registered for the same node or edge, they are executed in a specific order:

*   **`BeforeCall` Hooks**: Executed in a **LIFO** (Last-In, First-Out) order. The last hook added is the first one to be executed.
*   **`AfterCall` Hooks**: Also executed in a **LIFO** order.
*   **`WrapCall` Hooks**: Executed in a **FIFO** (First-In, First-Out) order. The first hook added is the first one to wrap the action, meaning its "before" logic runs first, and its "after" logic runs last.

## Code Examples

### Example 1: Node Hooks

This example is based on the `testNestedNodeWrapHooks` test case. It demonstrates the use of global `BeforeCall`, `AfterCall`, and `WrapCall` hooks on nodes. We'll use a helper class `NestedNodeHook` that records its execution in the state.

Here is the implementation of the `NestedNodeHook` helper class:

```java
package org.bsc.langgraph4j.hook;

import org.bsc.langgraph4j.Logging;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

public record NestedNodeHook<State extends AgentState>(String level, Map<String, Channel<?>> schema )
        implements NodeHook.WrapCall<State>, NodeHook.BeforeCall<State>, NodeHook.AfterCall<State>, Logging {

    public static final String AFTER_CALL_ATTRIBUTE = "node-after-call";
    public static final String HOOKS_ATTRIBUTE = "node-hooks";

    public NestedNodeHook(String level ) {
        this( level, null);
    }

    @Override
    public CompletableFuture<Map<String, Object>> applyWrap(State state, RunnableConfig config, AsyncNodeActionWithConfig<State> action) {

        log.info("node wrap call start: hook on '{}' level '{}'", config.nodeId(), level);
        return action.apply(state, config)
                .thenApply(result -> (schema!=null) ?
                        AgentState.updateState( result, Map.of(HOOKS_ATTRIBUTE, Map.of( config.nodeId(), List.of(level))), schema ) :
                        result )
                .whenComplete( ( result, exception ) -> {
                    log.info("node wrap call end: hook on '{}' level '{}'", config.nodeId(), level);
                });
    }

    @Override
    public CompletableFuture<Map<String, Object>> applyBefore(State state, RunnableConfig config) {
        log.info("node before call start: hook on '{}' level '{}'", config.nodeId(), level);
        return completedFuture( Map.<String,Object>of(HOOKS_ATTRIBUTE, Map.of( config.nodeId(), List.of(level))))
                .whenComplete( ( result, exception ) ->
                    log.info("node before call end: hook on '{}' level '{}'", config.nodeId(), level));

    }

    @Override
    public CompletableFuture<Map<String, Object>> applyAfter(State state, RunnableConfig config, Map<String, Object> lastResult) {
        log.info("node after call start: hook on '{}' level '{}'", config.nodeId(), level);
        return completedFuture( (schema!=null) ?
                AgentState.updateState( lastResult, Map.of( AFTER_CALL_ATTRIBUTE, 1), schema ) :
                Map.<String,Object>of())
                .whenComplete( ( result, exception ) ->
                    log.info("node after call end: hook on '{}' level '{}'", config.nodeId(), level));
    }
}
```

Now, let's see how to use it in a graph definition:

```java
final Map<String,Channel<?>> schema = mergeMap( MessagesState.SCHEMA,
        Map.of( NestedNodeHook.HOOKS_ATTRIBUTE, new RegisterHookChannel() ));

var workflow = new StateGraph<>(schema, State::new)
        .addWrapCallNodeHook( new NestedNodeHook<>("wrap-global-1", schema))
        .addBeforeCallNodeHook( new NestedNodeHook<>("before-global-1"))
        .addBeforeCallNodeHook( new NestedNodeHook<>("before-global-2"))
        .addAfterCallNodeHook( new NestedNodeHook<>("after-global-1"))
        .addNode("node_1", actionBuilder().nodeId("node_1").build() )
        .addNode("node_2", actionBuilder().nodeId("node_2").build() )
        .addNode("node_3", actionBuilder().nodeId("node_3").build() )
        .addNode("node_4", actionBuilder().nodeId("node_4").build() )
        .addEdge(START, "node_1")
        .addEdge("node_1", "node_2")
        .addEdge("node_2", "node_3")
        .addEdge("node_3", "node_4")
        .addEdge("node_4", END)
        .compile();

var result = workflow.invoke(   GraphInput.args(Map.of("input", "test1")),
        RunnableConfig.builder().build());

var state = result.get();
// The state will contain the execution trace of the hooks for each node.
// For "node_1", the trace would be: ["before-global-2", "before-global-1", "wrap-global-1"]
// This demonstrates the LIFO order for BeforeCall and FIFO for WrapCall.
```

### Example 2: Node and Edge Hooks

This example is based on the `testNestedNodeAndEdgeWrapHooks` test case. It shows how to use both node and edge hooks together. We will use an `AfterCall` edge hook to dynamically redirect the graph to the `END` node.

Here is the implementation of the `NestedEdgeHook` helper class:

```java
package org.bsc.langgraph4j.hook;

import org.bsc.langgraph4j.Logging;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.AsyncCommandAction;
import org.bsc.langgraph4j.action.Command;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

public record NestedEdgeHook<State extends AgentState>(String level, Map<String, Channel<?>> schema )
        implements EdgeHook.WrapCall<State>, EdgeHook.BeforeCall<State>, EdgeHook.AfterCall<State>, Logging {

    public static final String AFTER_CALL_ATTRIBUTE = "edge-after-call";
    public static final String HOOKS_ATTRIBUTE = "edge-hooks";

    public NestedEdgeHook(String level ) {
        this( level, null);
    }

    @Override
    public CompletableFuture<Command> applyAfter(State state, RunnableConfig config, Command lastResult) {
        log.info("edge after call start: hook on '{}' level '{}'", config.nodeId(), level);
        return completedFuture( (schema!=null) ?
                new Command( lastResult.gotoNodeSafe().orElse(null),
                        AgentState.updateState( lastResult.update(), Map.of(AFTER_CALL_ATTRIBUTE, 1), schema )) :
                lastResult)
                .whenComplete( ( result, exception ) ->
                        log.info("edge after call end: hook on '{}' level '{}'", config.nodeId(), level));

    }

    @Override
    public CompletableFuture<Command> applyBefore(State state, RunnableConfig config) {
        log.info("edge before call start: hook on '{}' level '{}'", config.nodeId(), level);
        return completedFuture( new Command( Map.of(HOOKS_ATTRIBUTE, Map.of( config.nodeId(), List.of(level)))))
                .whenComplete( ( result, exception ) ->
                        log.info("edge before call end: hook on '{}' level '{}'", config.nodeId(), level));

    }

    @Override
    public CompletableFuture<Command> applyWrap(State state, RunnableConfig config, AsyncCommandAction<State> action) {
        log.info("edge wrap call start: hook on '{}' level '{}'", config.nodeId(), level);
        return action.apply(state, config)
                .thenApply(result -> (schema!=null) ?
                        new Command( result.gotoNodeSafe().orElse(null),
                                AgentState.updateState( result.update(),
                                                        Map.of(HOOKS_ATTRIBUTE, Map.of( config.nodeId(), List.of(level))), schema )) :
                        result )
                .whenComplete( ( result, exception ) -> {
                    log.info("edge wrap call end: hook on '{}' level '{}'", config.nodeId(), level);
                });

    }
}
```

And here is the graph definition with both node and edge hooks:

```java
final Map<String,Channel<?>> schema = mergeMap( MessagesState.SCHEMA,
        Map.of( NestedNodeHook.HOOKS_ATTRIBUTE, new RegisterHookChannel(),
                NestedEdgeHook.HOOKS_ATTRIBUTE, new RegisterHookChannel() ));

EdgeHook.AfterCall<State> afterEdgeHookGoToEnd = ( s, c, lastResult ) ->
    completedFuture(new Command( END, lastResult.update() ));

var workflow = new StateGraph<>(schema, State::new)
        .addWrapCallNodeHook( new NestedNodeHook<>("wrap-global-1", schema))
        .addBeforeCallNodeHook( new NestedNodeHook<>("before-global-1"))
        .addBeforeCallNodeHook( new NestedNodeHook<>("before-global-2"))
        .addAfterCallNodeHook( new NestedNodeHook<>("after-global-1"))
        .addAfterCallEdgeHook( "node_2", afterEdgeHookGoToEnd )
        .addNode("node_1", actionBuilder().nodeId("node_1").build() )
        .addNode("node_2", actionBuilder().nodeId("node_2").build() )
        .addNode("node_3", actionBuilder().nodeId("node_3").build() )
        .addNode("node_4", actionBuilder().nodeId("node_4").build() )
        .addEdge(START, "node_1")
        .addEdge("node_1", "node_2")
        .addConditionalEdges("node_2",
                command_async( ( s, c ) -> new Command("node_3")),
                EdgeMappings.builder()
                        .to("node_3")
                        .toEND()
                        .build())
        .addEdge("node_3", "node_4")
        .addEdge("node_4", END)
        .compile();

var result = workflow.invoke(   GraphInput.args(Map.of("input", "test1")),
        RunnableConfig.builder().build());

var state = result.get();
// Because of the 'afterEdgeHookGoToEnd' on "node_2", the graph execution will stop after "node_2".
// The messages in the state will be: ["node_1", "node_2"]
```

This documentation should give you a comprehensive understanding of how to use hooks in LangGraph4j.
