package org.bsc.langgraph4j.hook;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.AsyncNodeActionWithConfig;
import org.bsc.langgraph4j.state.AgentState;

import java.util.*;
import java.util.concurrent.CompletableFuture;

public interface NodeHook {

    interface BeforeCall<State extends AgentState> {
        CompletableFuture<Map<String, Object>> accept(State state, RunnableConfig config );
    }

    interface AfterCall<State extends AgentState> {
        CompletableFuture<Map<String, Object>> accept(State state, RunnableConfig config, Map<String, Object> result ) ;
    }

    interface WrapCall<State extends AgentState> {
        CompletableFuture<Map<String, Object>> apply(State state, RunnableConfig config, AsyncNodeActionWithConfig<State> action);
    }


}

