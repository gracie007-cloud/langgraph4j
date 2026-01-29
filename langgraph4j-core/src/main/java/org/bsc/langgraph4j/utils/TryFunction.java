package org.bsc.langgraph4j.utils;

import java.util.function.Function;

import org.bsc.langgraph4j.LG4JLoggable;

@FunctionalInterface
public interface TryFunction<T, R, Ex extends Throwable> extends Function<T,R>, LG4JLoggable {

    R tryApply( T t ) throws Ex;

    default R apply( T t ) {
        try {
            return tryApply(t);
        } catch (Throwable ex) {
            log.error( ex.getMessage(), ex );
            throw new RuntimeException(ex);
        }
    }

    static <T,R,Ex extends Throwable> Function<T,R> Try( TryFunction<T,R,Ex> function ) {
        return function;
    }
}
