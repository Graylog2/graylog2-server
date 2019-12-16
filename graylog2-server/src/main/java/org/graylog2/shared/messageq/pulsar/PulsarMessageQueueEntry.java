package org.graylog2.shared.messageq.pulsar;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.graylog2.shared.messageq.MessageQueue;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class PulsarMessageQueueEntry implements MessageQueue.Entry {
    private final byte[] id;
    private final byte[] key;
    private final byte[] value;
    private final long timestamp;

    PulsarMessageQueueEntry(byte[] id, @Nullable byte[] key, byte[] value, long timestamp) {
        this.id = requireNonNull(id, "id cannot be null");
        this.key = key;
        this.value = requireNonNull(value, "value cannot be null");
        this.timestamp = timestamp;
    }

    @Override
    public byte[] id() {
        return id;
    }

    @Nullable
    @Override
    public byte[] key() {
        return key;
    }

    @Override
    public byte[] value() {
        return value;
    }

    @Override
    public long timestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id.len", id.length)
                .add("key.len", key != null ? key.length : key)
                .add("value.len", value.length)
                .add("timestamp", timestamp)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PulsarMessageQueueEntry that = (PulsarMessageQueueEntry) o;
        return timestamp == that.timestamp &&
                Objects.equal(id, that.id) &&
                Objects.equal(key, that.key) &&
                Objects.equal(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, key, value, timestamp);
    }
}
