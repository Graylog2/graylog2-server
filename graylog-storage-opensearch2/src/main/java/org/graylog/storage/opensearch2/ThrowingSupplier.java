package org.graylog.storage.opensearch2;

public interface ThrowingSupplier<R, E extends Exception> {
    R get() throws E;
}
