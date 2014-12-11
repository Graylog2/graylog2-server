package org.graylog2.plugin.system;

/**
 * Created by dennis on 11/12/14.
 */
public class NodeIdPersistenceException extends RuntimeException {
    public NodeIdPersistenceException() {
        super();
    }

    public NodeIdPersistenceException(String message) {
        super(message);
    }

    public NodeIdPersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    public NodeIdPersistenceException(Throwable cause) {
        super(cause);
    }
}
