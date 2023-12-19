package org.graylog.storage.opensearch2;

public interface ThrowingFunction<A, R, E extends Exception> {
    R apply(A a) throws E;
}
