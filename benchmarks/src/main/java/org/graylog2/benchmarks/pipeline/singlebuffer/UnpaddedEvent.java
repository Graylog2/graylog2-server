package org.graylog2.benchmarks.pipeline.singlebuffer;

import com.lmax.disruptor.EventFactory;

public class UnpaddedEvent implements Event {
    private ProcessedMessage rawMessage;
    private ProcessedMessage outputMessage;

    public static final EventFactory<Event> FACTORY = new EventFactory<Event>() {
        @Override
        public Event newInstance() {
            return new UnpaddedEvent();
        }
    };

    @Override
    public ProcessedMessage getRawMessage() {
        return rawMessage;
    }

    @Override
    public void setRawMessage(ProcessedMessage rawMessage) {
        this.rawMessage = rawMessage;
    }

    @Override
    public ProcessedMessage getOutputMessage() {
        return outputMessage;
    }

    @Override
    public void setOutputMessage(ProcessedMessage outputMessage) {
        this.outputMessage = outputMessage;
    }
}
