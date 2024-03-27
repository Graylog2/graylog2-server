package org.graylog.storage.opensearch2;

public class RemoteReindexNotAllowedException extends IllegalStateException {
    public RemoteReindexNotAllowedException(String message) {
        super(message);
    }
}
