package org.graylog2.benchmarks.pipeline.singlebuffer;

import com.lmax.disruptor.EventFactory;

public class PaddedEvent implements Event {
    private ProcessedMessage[] padded = new ProcessedMessage[128];

    public static final EventFactory<Event> FACTORY = new EventFactory<Event>() {
        @Override
        public Event newInstance() {
            return new PaddedEvent();
        }
    };

    @Override
    public ProcessedMessage getRawMessage() {
        return padded[0];
    }

    @Override
    public void setRawMessage(ProcessedMessage rawMessage) {
        this.padded[0] = rawMessage;
    }

    @Override
    public ProcessedMessage getOutputMessage() {
        return padded[64];
    }

    @Override
    public void setOutputMessage(ProcessedMessage outputMessage) {
        this.padded[64] = outputMessage;
    }
}
