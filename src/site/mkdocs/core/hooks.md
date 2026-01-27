# Graph Hooks

Hooks in LangGraph4j provide a powerful mechanism to intercept the execution of graph nodes and edges. They allow you to run custom code **before**, **after**, or **around (wrap)** the core logic of a node or a conditional edge.

## Hook Interfaces

### Node Hooks

The `org.bsc.langgraph4j.hook.NodeHook` interface defines hooks related to the graph's nodes. There are three types:

* **`BeforeCall`**: Executed before the node action.
* **`AfterCall`**: Executed after the node action.
* **`WrapCall`**: Wraps the node action, allowing for custom logic before and after execution.

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

1. **Global Hooks**: Affect all nodes or all conditional edges in the graph.
2. **Specific Hooks**: Affect only a specific node or conditional edge identified by its ID.

### Registration Examples

```java
// Register a global node hook
stateGraph.addBeforeCallNodeHook( (nodeId, state, config) -> {
    System.out.println("Before node: " + nodeId);
    return CompletableFuture.completedFuture(Map.of());
});

// Register a node hook for a specific node
stateGraph.addAfterCallNodeHook("agent_1", (nodeId, state, config, lastResult) -> {
    System.out.println("After agent_1 completed");
    return CompletableFuture.completedFuture(lastResult);
});

// Register a global edge hook
stateGraph.addWrapCallEdgeHook( (sourceId, state, config, action) -> {
    System.out.println("Wrapping edge from: " + sourceId);
    return action.apply(state, config);
});
```

## Execution Order and Strategy

When multiple hooks are registered (global and/or specific), their execution order is determined by the following strategies:

* **Before Call Hooks**: Executed using a **LIFO** (Last Input - Last Output) strategy. The most recently added hook executes first.
* **After Call Hooks**: Executed using a **LIFO** (Last Input - Last Output) strategy. The most recently added hook executes first.
* **Wrap Call Hooks**: Executed using a **FIFO** (First Input - First Out) strategy. The first hook added becomes the outermost wrapper.

### Example: Execution Trace

The unit test `testNestedNodeWrapHooks` in `GraphTest.java` demonstrates this behavior. Suppose you have the following setup:

```java
var workflow = new StateGraph<>(schema, State::new)
    .addWrapCallNodeHook(new NestedNodeHook<>("wrap-global-1", schema))
    .addBeforeCallNodeHook(new NestedNodeHook<>("before-global-1"))
    .addBeforeCallNodeHook(new NestedNodeHook<>("before-global-2"))
    .addAfterCallNodeHook(new NestedNodeHook<>("after-global-1"))
    .addNode("node_1", ... )
    .compile();
```

For `node_1`, the execution sequence will be:

1. **`before-global-2`** (LIFO: added last, runs first)
2. **`before-global-1`** (LIFO: added first, runs second)
3. **`wrap-global-1`** (FIFO: outermost wrapper)
4. **(Node logic)**
5. **`after-global-1`** (LIFO)

## Use Cases

* **Tracing/Logging**: Capture the entry and exit of every node.
* **State Transformation**: Modify the state before it reaches a node or before it is saved.
* **Dynamic Routing**: Use edge hooks to override or log conditional routing decisions.
* **Performance Monitoring**: Measure the execution time of nodes using wrap hooks.

For practical implementations and advanced nested hook examples, see the `testNestedNodeWrapHooks` and `testNestedNodeAndEdgeWrapHooks` methods in `langgraph4j-core/src/test/java/org/bsc/langgraph4j/GraphTest.java`.
