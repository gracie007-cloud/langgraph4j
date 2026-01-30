package org.bsc.langgraph4j.utils;

import java.util.function.Consumer;

import org.bsc.langgraph4j.LG4JLoggable;

@FunctionalInterface
public interface TryConsumer<T, Ex extends Throwable> extends Consumer<T>, LG4JLoggable {

    void tryAccept( T t ) throws Ex;

    default void accept( T t ) {
        try {
            tryAccept(t);
        } catch (Throwable ex) {
            log.error( ex.getMessage(), ex );
            throw new RuntimeException(ex);
        }
    }

    static <T,Ex extends Throwable> Consumer<T> Try( TryConsumer<T, Ex> consumer ) {
        return consumer;
    }
}
