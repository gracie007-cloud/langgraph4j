# LangGraph4j - Graph Execution Cancellation

LangGraph4j provides a powerful mechanism to cancel the execution of a graph, which is particularly useful for long-running processes or when an execution path is no longer needed. 

The cancellation feature in LangGraph4j relies on the **AsyncGenerator** cancellation implementation provided by the `java-async-generator` library. For detailed information on the underlying mechanism, see [AsyncGenerator Cancellation](https://github.com/bsorrentino/java-async-generator/blob/main/CANCELLATION.md).

## How it Works

When you execute a graph using the `stream` method, it returns an `AsyncGenerator.Cancellable` instance. This object allows you to trigger cancellation and check the current cancellation status.

### The `cancel` Method

The core of the cancellation feature is the `cancel(boolean mayInterruptIfRunning)` method:

*   **`mayInterruptIfRunning = true`**: Triggers an immediate cancellation. It attempts to interrupt the thread currently executing a node. If you are consuming the stream with `forEachAsync`, this will result in the `CompletableFuture` completing exceptionally with an `InterruptedException`.
*   **`mayInterruptIfRunning = false`**: Triggers a "graceful" cancellation. The system will wait for the currently executing node to complete its task before stopping the graph execution.

## Usage Examples

The following examples demonstrate how to use the cancellation feature in different scenarios.

### Immediate Cancellation using `forEachAsync`

When using `forEachAsync`, the graph runs in its own execution context. Requests to cancel are typically made from a different thread.

```java
var generator = workflow.stream(GraphInput.noArgs(), RunnableConfig.builder().build());

// Request cancellation from a different thread
CompletableFuture.runAsync(() -> {
    try {
        Thread.sleep(50); // Small delay to let execution start
        log.info("Requesting immediate cancellation...");
        boolean result = generator.cancel(true); // mayInterruptIfRunning = true
        log.info("Cancellation request result: {}", result);
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
});

var futureResult = generator.forEachAsync(output -> {
    log.info("Processing output: {}", output);
}).exceptionally(ex -> {
    // Check if the cause of exception is an InterruptedException
    if (generator.isCancelled()) {
        log.info("Graph execution was cancelled!");
        return "CANCELLED";
    }
    throw new RuntimeException(ex);
});

String result = futureResult.get(5, TimeUnit.SECONDS);
assertEquals("CANCELLED", result);
```

### Graceful Cancellation using Iterator

If you are iterating over the generator directly, the loop will terminate once the current step finishes after a cancellation request.

```java
var generator = workflow.stream(GraphInput.noArgs(), RunnableConfig.builder().build());

// Request cancellation
CompletableFuture.runAsync(() -> {
    try {
        Thread.sleep(100);
        generator.cancel(false); // mayInterruptIfRunning = false
    } catch (InterruptedException e) {
        throw new RuntimeException(e);
    }
});

NodeOutput<?> lastOutput = null;
for (var output : generator) {
    log.info("Received output from node: {}", output.node());
    lastOutput = output;
}

// After the loop terminates
assertTrue(generator.isCancelled());
assertTrue(GraphResult.from(generator).isEmpty());
```

## Advanced Scenarios

### Parallel Node Cancellation

LangGraph4j supports parallel node execution. When a graph is cancelled, all currently running parallel tasks are notified. If `mayInterruptIfRunning` is set to `true`, the `shutdownNow()` method is typically called on the associated executor to stop running tasks immediately.

### Subgraph Propagation

Cancellation is recursive. If a parent graph is cancelled while a subgraph is executing, the cancellation is automatically propagated down to the subgraph's generator.

## Key API Summary

| Method | Description |
| :--- | :--- |
| `cancel(boolean mayInterrupt)` | Triggers cancellation. Returns `true` if the request was successful. |
| `isCancelled()` | Returns `true` if a cancellation request has been made. |
| `GraphResult.from(generator)` | Utility to check if a result was reached before cancellation. |

For developers interested in the low-level implementation, refer to the [java-async-generator](https://github.com/bsorrentino/java-async-generator) documentation.

