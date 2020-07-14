package org.graylog.storage.elasticsearch7;

public interface ThrowingBiFunction<A1, A2, R, E extends Exception> {
    R apply(A1 a1, A2 a2) throws E;
}
