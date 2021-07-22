package org.graylog.failure;

public interface ProcessingFailureRoutingConfiguration {

    boolean writeOriginalMessageWithError();

    boolean submitProcessingFailure();
}
