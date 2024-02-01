package org.graylog2.system.processing.control;

public class ClusterProcessingControlException extends RuntimeException {

    public ClusterProcessingControlException(String message) {
        super(message);
    }

    public ClusterProcessingControlException(String message, Throwable cause) {
        super(message, cause);
    }
}
