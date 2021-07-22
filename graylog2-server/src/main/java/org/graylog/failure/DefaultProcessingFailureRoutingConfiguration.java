package org.graylog.failure;

public class DefaultProcessingFailureRoutingConfiguration implements ProcessingFailureRoutingConfiguration{

    @Override
    public boolean writeOriginalMessageWithError() {
        return true;
    }

    @Override
    public boolean submitProcessingFailure() {
        return false;
    }
}
