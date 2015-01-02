package org.graylog2.plugin.events.inputs;

import com.google.auto.value.AutoValue;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.Stoppable;

@AutoValue
public abstract class IOStateChangedEvent<T extends Stoppable> {
    public abstract IOState.Type oldState();
    public abstract IOState.Type newState();
    public abstract IOState<T> changedState();

    public static <K extends Stoppable> IOStateChangedEvent<K> create(IOState.Type oldState, IOState.Type newState, IOState<K> changedEvent) {
        return new AutoValue_IOStateChangedEvent<>(oldState, newState, changedEvent);
    }
}
