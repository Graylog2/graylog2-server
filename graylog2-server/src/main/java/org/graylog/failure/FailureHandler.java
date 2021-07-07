package org.graylog.failure;

public interface FailureHandler {

    void handle(Failure failure);

    boolean supports(Failure failure);

    boolean isEnabled();
}
