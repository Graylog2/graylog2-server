package org.graylog2.indexer;

public class CircuitBreakerException extends ElasticsearchException {
    public CircuitBreakerException(String message) {
        super(message);
    }
}
