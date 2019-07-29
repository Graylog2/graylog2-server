package org.graylog.events.event;

import com.google.auto.value.AutoValue;
import org.graylog2.plugin.Message;

import javax.annotation.Nullable;
import java.util.Optional;

@AutoValue
public abstract class EventWithContext {
    public abstract Event event();

    public abstract Optional<Message> messageContext();

    public abstract Optional<Event> eventContext();

    public static EventWithContext create(Event event) {
        return builder().event(event).build();
    }

    public static EventWithContext create(Event event, Message messageContext) {
        return builder().event(event).messageContext(messageContext).build();
    }

    public static EventWithContext create(Event event, Event eventContext) {
        return builder().event(event).eventContext(eventContext).build();
    }

    public EventWithContext addMessageContext(Message message) {
        return toBuilder().messageContext(message).build();
    }

    public EventWithContext addEventContext(Event event) {
        return toBuilder().eventContext(event).build();
    }

    public static Builder builder() {
        return new AutoValue_EventWithContext.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder event(Event event);

        public abstract Builder messageContext(@Nullable Message message);

        public abstract Builder eventContext(@Nullable Event event);

        public abstract EventWithContext build();
    }
}