package org.graylog2.failure;

public interface FailureHandler {

    void handle(Failure failure);

    boolean supports(Failure failure);
}
