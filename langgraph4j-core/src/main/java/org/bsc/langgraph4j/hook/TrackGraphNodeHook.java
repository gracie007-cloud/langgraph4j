package org.bsc.langgraph4j.hook;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.state.AgentState;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

public class TrackGraphNodeHook<State extends AgentState> implements NodeHooks.WrapCall<State> {

    private final String nodeId;

    public TrackGraphNodeHook(String nodeId) {
        this.nodeId = nodeId;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(State state, RunnableConfig config, AsyncNodeActionWithConfig<State> action) {

        return action.apply(state, RunnableConfig.builder(config)
                                        .putMetadata( "langgraph_node", nodeId)
                                        .build());
    }
}
