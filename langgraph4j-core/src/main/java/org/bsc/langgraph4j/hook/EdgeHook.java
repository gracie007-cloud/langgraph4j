package org.bsc.langgraph4j.hook;

import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.action.AsyncCommandAction;
import org.bsc.langgraph4j.action.Command;
import org.bsc.langgraph4j.state.AgentState;

import java.util.concurrent.CompletableFuture;

public interface EdgeHook {

    interface BeforeCall<State extends AgentState> {
        CompletableFuture<Command> applyBefore(State state, RunnableConfig config );
    }

    interface AfterCall<State extends AgentState> {
        CompletableFuture<Command> applyAfter(State state, RunnableConfig config, Command lastResult ) ;
    }

    interface WrapCall<State extends AgentState> {
        CompletableFuture<Command> applyWrap(State state, RunnableConfig config, AsyncCommandAction<State> action);
    }

}
