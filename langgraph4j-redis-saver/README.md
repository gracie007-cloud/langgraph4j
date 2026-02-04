# langgraph4j-redis-saver

Redis-based checkpoint saver for LangGraph4j. Provides high-performance, persistent storage of workflow state using Redis as the backend.

## Features

- **High Performance**: In-memory Redis operations for sub-millisecond checkpoint access
- **Two Configuration Modes**: Direct Redis configuration or inject an existing RedissonClient
- **Customizable Key Naming**: Implement your own `KeyNamingStrategy` for integration with existing Redis key patterns
- **Atomic Operations**: Uses Redisson batches for thread-safe checkpoint operations
- **Thread Management**: Soft-delete mechanism for thread release

## Maven Dependency

```xml
<dependency>
    <groupId>org.bsc.langgraph4j</groupId>
    <artifactId>langgraph4j-redis-saver</artifactId>
    <version>1.8-SNAPSHOT</version>
</dependency>

```

## Usage

### Mode 1: Direct Configuration

Configure Redis connection parameters directly:

```java
var saver = RedisSaver.builder()
        .host("localhost")
        .port(6379)
        .password("your-password")
        .database(0)
        .build();

var graph = new StateGraph<>(AgentState::new)
        .addNode("agent_1", node_async(myAction))
        .addEdge(START, "agent_1")
        .addEdge("agent_1", END);

var compileConfig = CompileConfig.builder()
        .checkpointSaver(saver)
        .build();

var workflow = graph.compile(compileConfig);
```

### Mode 2: Inject RedissonClient

Reuse an existing RedissonClient from your application:

```java
Config config = new Config();
config.useSingleServer()
        .setAddress("redis://localhost:6379")
        .setPassword("your-password");

RedissonClient redissonClient = Redisson.create(config);

var saver = RedisSaver.builder()
        .redissonClient(redissonClient)
        .build();

```

### Builder Options

#### Direct Configuration Mode

| Option | Type | Default | Description |
|--------|------|---------|-------------|
| `host(String)` | String | "localhost" | Redis host |
| `port(int)` | int | 6379 | Redis port |
| `username(String)` | String | null | Redis username (for Redis 6+ ACL) |
| `password(String)` | String | null | Redis password |
| `database(int)` | int | 0 | Redis database index |
| `connectionTimeout(int)` | int | 3000 | Connection timeout (ms) |
| `retryInterval(int)` | int | 1500 | Retry interval (ms) |
| `retryAttempts(int)` | int | 3 | Number of retry attempts |
| `keyNamingStrategy(KeyNamingStrategy)` | KeyNamingStrategy | DefaultKeyNamingStrategy | Custom key naming strategy |

#### RedissonClient Injection Mode

| Option | Type | Required | Description |
|--------|------|----------|-------------|
| `redissonClient(RedissonClient)` | RedissonClient | Yes | Existing RedissonClient to reuse |
| `keyNamingStrategy(KeyNamingStrategy)` | KeyNamingStrategy | No | Custom key naming strategy |

## Custom Key Naming Strategy

Implement the `KeyNamingStrategy` interface to customize Redis key patterns:

```java
KeyNamingStrategy customNaming = new KeyNamingStrategy() {
    @Override
    public String threadKey(String threadId) {
        return "myapp:threads:" + threadId;
    }

    @Override
    public String threadNameKey(String threadName) {
        return "myapp:thread_names:" + threadName;
    }

    @Override
    public String checkpointKey(String checkpointId) {
        return "myapp:checkpoints:" + checkpointId;
    }

    @Override
    public String checkpointsKey(String threadId) {
        return "myapp:thread_checkpoints:" + threadId;
    }

    @Override
    public String keyPrefix() {
        return "myapp:";
    }
};

var saver = RedisSaver.builder()
        .redissonClient(redissonClient)
        .keyNamingStrategy(customNaming)
        .build();
```

## Redis Data Structures

This module uses the following Redis data structures (with default key naming):

| Key Pattern | Type | Purpose |
|-------------|------|---------|
| `langgraph4j:thread:{thread_id}` | Hash | Thread metadata (thread_id, thread_name, is_released, created_at) |
| `langgraph4j:thread:name:{thread_name}:active` | String | Active thread lookup by name |
| `langgraph4j:checkpoint:{checkpoint_id}` | Hash | Checkpoint data (checkpoint_id, thread_id, node_id, next_node_id, state_data, saved_at) |
| `langgraph4j:thread:{thread_id}:checkpoints` | Sorted Set | Ordered checkpoint IDs by timestamp (score) |



```java
var saver = RedisSaver.builder()
        .redissonClient(myRedissonClient)
        .build();
```

## Comparison with Relational Savers

| Feature | MySQL/PostgreSQL/Oracle | Redis (Redisson) |
|---------|-------------------------|------------------|
| **Performance** | Medium (disk I/O) | Very Fast (in-memory) |
| **Persistence** | Durable (ACID) | Durable (with Redis persistence config) |
| **Schema** | Tables (DDL required) | No schema (dynamic keys) |
| **Transactions** | ACID | Atomic (via Redisson batches) |
| **JSON Support** | Native JSON columns | String (JSON via Jackson) |
| **Scalability** | Vertical scaling | Horizontal (Redis Cluster) |
| **Use Case** | Traditional apps | Caching, real-time, high-throughput |

## License

This module is part of the LangGraph4j project.
