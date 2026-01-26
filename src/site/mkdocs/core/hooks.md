
# Graph Hooks

Hooks in LangGraph4j provide a powerful mechanism to intercept the execution of graph nodes and edges. They allow you to run custom code **before**, **after**, or **around** (wrap) the core logic of a node or a conditional edge.

## Hook Interfaces

### Node Hooks
The `org.bsc.langgraph4j.hook.NodeHook` interface defines hooks related to the graph's nodes. There are three types:

*   **`BeforeCall`**: Executed before the node action. 
*   **`AfterCall`**: Executed after the node action.
*   **`WrapCall`**: Wraps the node action, allowing for custom logic before and after execution.

```java
public interface NodeHook {
    @FunctionalInterface
    interface BeforeCall<State extends AgentState> {
        CompletableFuture<Map<String, Object>> applyBefore(String nodeId, State state, RunnableConfig config);
    }

    @FunctionalInterface
    interface AfterCall<State extends AgentState> {
        CompletableFuture<Map<String, Object>> applyAfter(String nodeId, State state, RunnableConfig config, Map<String, Object> lastResult);
    }

    @FunctionalInterface
    interface WrapCall<State extends AgentState> {
        CompletableFuture<Map<String, Object>> applyWrap(String nodeId, State state, RunnableConfig config, AsyncNodeActionWithConfig<State> action);
    }
}
```

### Edge Hooks
The `org.bsc.langgraph4j.hook.EdgeHook` interface defines hooks related to the graph's conditional edges. Note that only conditional edges support hooks in LangGraph4j.

```java
public interface EdgeHook {
    @FunctionalInterface
    interface BeforeCall<State extends AgentState> {
        CompletableFuture<Command> applyBefore(String sourceId, State state, RunnableConfig config);
    }

    @FunctionalInterface
    interface AfterCall<State extends AgentState> {
        CompletableFuture<Command> applyAfter(String sourceId, State state, RunnableConfig config, Command lastResult);
    }

    @FunctionalInterface
    interface WrapCall<State extends AgentState> {
        CompletableFuture<Command> applyWrap(String sourceId, State state, RunnableConfig config, AsyncCommandAction<State> action);
    }
}
```

## Registering Hooks

Hooks are registered using the `StateGraph` class. You can register them in two scopes:

1.  **Global Hooks**: Affect all nodes or all conditional edges in the graph.
2.  **Specific Hooks**: Affect only a specific node or conditional edge identified by its ID.

### Registration Methods

```java
// Global Node Hooks
stateGraph.addBeforeCallNodeHook(BeforeNodeHook);
stateGraph.addAfterCallNodeHook(AfterNodeHook);
stateGraph.addWrapCallNodeHook(WrapNodeHook);

// Specific Node Hooks
stateGraph.addBeforeCallNodeHook("node_id", BeforeNodeHook);

// Global Edge Hooks
stateGraph.addAfterCallEdgeHook(AfterEdgeHook);

// Specific Edge Hooks
stateGraph.addAfterCallEdgeHook("node_id", AfterEdgeHook);
```

## Execution Order and Strategy

When multiple hooks are registered (global and/or specific), their execution order is determined by the following strategies:

*   **Before Call Hooks**: Executed using a **LIFO** (Last-In, First-Out) strategy. The most recently added hook runs first.
*   **After Call Hooks**: Executed using a **LIFO** (Last-In, First-Out) strategy.
*   **Wrap Call Hooks**: Executed using a **FIFO** (First-In, First-Out) strategy. The first hook added is the outermost wrapper.

### Example: Nested Tracing
The `GraphTest.java` file contains examples like `testNestedNodeWrapHooks` and `testNestedNodeAndEdgeWrapHooks` which demonstrate how hooks can be used to build a trace of execution:

```java
var workflow = new StateGraph<>(schema, State::new)
    .addWrapCallNodeHook(new NestedNodeHook<>("wrap-global-1", schema))
    .addBeforeCallNodeHook(new NestedNodeHook<>("before-global-1"))
    .addBeforeCallNodeHook(new NestedNodeHook<>("before-global-2"))
    .addAfterCallNodeHook(new NestedNodeHook<>("after-global-1"))
    .addNode("node_1", ... )
    .compile();
```

In this setup, for `node_1`, the execution order would be:
1. `before-global-2` (LIFO)
2. `before-global-1` (LIFO)
3. `wrap-global-1` (FIFO wrapper begins)
4. (Node logic)
5. `after-global-1` (LIFO)

## Further Reading
For practical implementations, refer to the following tests in the codebase:
* `testNestedNodeWrapHooks`
* `testNestedNodeAndEdgeWrapHooks`
in `langgraph4j-core/src/test/java/org/bsc/langgraph4j/GraphTest.java`.



