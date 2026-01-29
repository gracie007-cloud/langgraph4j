package org.bsc.langgraph4j.utils;

import java.util.function.Supplier;

import org.bsc.langgraph4j.LG4JLoggable;

@FunctionalInterface
public interface TrySupplier<R, Ex extends Throwable> extends Supplier<R>, LG4JLoggable {

    R tryGet() throws Ex;

    default R get() {
        try {
            return tryGet();
        } catch (Throwable ex) {
            log.error( ex.getMessage(), ex );
            throw new RuntimeException(ex);
        }
    }

    static <T,R,Ex extends Throwable> Supplier<R> Try( TrySupplier<R,Ex> supplier ) {
        return supplier;
    }
}
