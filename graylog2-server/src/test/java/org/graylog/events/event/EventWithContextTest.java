package org.graylog.events.event;

import org.graylog2.plugin.Message;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class EventWithContextTest {
    @Test
    public void createWithoutMessageAndEventContext() {
        final Event event = new TestEvent();
        final EventWithContext withContext = EventWithContext.builder()
                .event(event)
                .build();

        assertThat(withContext.event()).isEqualTo(event);
        assertThat(withContext.messageContext()).isNotPresent();
        assertThat(withContext.eventContext()).isNotPresent();
    }

    @Test
    public void createWithMessageAndWithoutEventContext() {
        final Event event = new TestEvent();
        final Message message = new Message("", "", DateTime.now(DateTimeZone.UTC));
        final EventWithContext withContext = EventWithContext.builder()
                .event(event)
                .messageContext(message)
                .build();

        assertThat(withContext.event()).isEqualTo(event);
        assertThat(withContext.messageContext()).get().isEqualTo(message);
        assertThat(withContext.eventContext()).isNotPresent();
    }

    @Test
    public void createWithEventAndWithoutMessageContext() {
        final Event event = new TestEvent();
        final Event eventContext = new TestEvent();
        final EventWithContext withContext = EventWithContext.builder()
                .event(event)
                .eventContext(eventContext)
                .build();

        assertThat(withContext.event()).isEqualTo(event);
        assertThat(withContext.messageContext()).isNotPresent();
        assertThat(withContext.eventContext()).get().isEqualTo(eventContext);
        assertThat(withContext.event()).isNotEqualTo(withContext.eventContext());
    }

    @Test
    public void addMessageContext() {
        final Event event = new TestEvent();
        final Message message = new Message("", "", DateTime.now(DateTimeZone.UTC));
        final EventWithContext withContext = EventWithContext.builder()
                .event(event)
                .build();

        final EventWithContext withContext1 = withContext.addMessageContext(message);

        assertThat(withContext.messageContext()).isNotPresent();
        assertThat(withContext1.messageContext()).get().isEqualTo(message);
    }

    @Test
    public void addEventContext() {
        final Event event = new TestEvent();
        final Event eventContext = new TestEvent();
        final EventWithContext withContext = EventWithContext.builder()
                .event(event)
                .build();

        final EventWithContext withContext1 = withContext.addEventContext(eventContext);

        assertThat(withContext.eventContext()).isNotPresent();
        assertThat(withContext1.eventContext()).get().isEqualTo(eventContext);
        assertThat(withContext1.event()).isNotEqualTo(withContext1.eventContext());
    }
}