package org.graylog2.benchmarks.pipeline.classic;

import com.lmax.disruptor.EventFactory;

public class Event {
    public ProcessedMessage message;

    public static final EventFactory<Event> FACTORY = new EventFactory<Event>() {
        @Override
        public Event newInstance() {
            return new Event();
        }
    };

}
