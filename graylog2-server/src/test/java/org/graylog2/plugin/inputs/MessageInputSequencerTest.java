package org.graylog2.plugin.inputs;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MessageInputSequencerTest {

    @Test
    public void sequencer() {
        final MessageInputSequencer sequencer = new MessageInputSequencer();

        assertThat(sequencer.next(1)).isEqualTo(0);
        assertThat(sequencer.next(1)).isEqualTo(1);
        assertThat(sequencer.next(1)).isEqualTo(2);

        // Sequence number gets reset for a new timestamp
        assertThat(sequencer.next(2)).isEqualTo(0);
        assertThat(sequencer.next(2)).isEqualTo(1);
        assertThat(sequencer.next(2)).isEqualTo(2);

        assertThat(sequencer.next(3)).isEqualTo(0);

        // Sequence should be reset even when timestamp goes backwards
        assertThat(sequencer.next(1)).isEqualTo(0);
    }

    @Test
    public void sequencerWrapping() {
        final MessageInputSequencer sequencer = new MessageInputSequencer(Integer.MAX_VALUE, 0L);

        // next() should never return a negative number and is reset to 0 if it would wrap to Integer.MIN_VALUE
        assertThat(sequencer.next(0)).isEqualTo(Integer.MAX_VALUE);
        assertThat(sequencer.next(0)).isEqualTo(0);
        assertThat(sequencer.next(0)).isEqualTo(1);

        assertThat(sequencer.next(1)).isEqualTo(0);
        assertThat(sequencer.next(1)).isEqualTo(1);
        assertThat(sequencer.next(1)).isEqualTo(2);
    }

    @Test
    public void invalidConstructor() {
        assertThatThrownBy(() -> new MessageInputSequencer(-1, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MessageInputSequencer(0, -1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new MessageInputSequencer(-1, -1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    public void invalidNextTimestamp() {
        final MessageInputSequencer sequencer = new MessageInputSequencer();

        assertThatThrownBy(() -> sequencer.next(-2L)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> sequencer.next(-1L)).isInstanceOf(IllegalArgumentException.class);
        assertThat(sequencer.next(0L)).isEqualTo(0);
        assertThat(sequencer.next(1L)).isEqualTo(0);
        assertThat(sequencer.next(2L)).isEqualTo(0);
    }
}
