package org.graylog2.benchmarks.pipeline.singlebuffer;

public interface Event {
    ProcessedMessage getRawMessage();

    void setRawMessage(ProcessedMessage rawMessage);

    ProcessedMessage getOutputMessage();

    void setOutputMessage(ProcessedMessage outputMessage);
}
