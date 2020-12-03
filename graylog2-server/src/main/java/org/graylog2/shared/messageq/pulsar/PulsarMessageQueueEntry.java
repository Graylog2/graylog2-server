package org.graylog2.shared.messageq.pulsar;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.apache.pulsar.client.api.Message;
import org.apache.pulsar.client.api.MessageId;
import org.graylog2.shared.messageq.MessageQueue;

import javax.annotation.Nullable;
import java.util.Arrays;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Objects.requireNonNull;

public class PulsarMessageQueueEntry implements MessageQueue.Entry {
    private final byte[] id;
    private final byte[] key;
    private final byte[] value;
    private final long timestamp;
    private final MessageId commitId;

    PulsarMessageQueueEntry(Message<byte[]> message) {
        this.commitId = requireNonNull(message.getMessageId(), "messageId cannot be null");
        this.id = commitId.toByteArray();
        this.key = message.getKey().getBytes(UTF_8);
        this.value = requireNonNull(message.getData(), "value cannot be null");
        this.timestamp = message.getEventTime();
    }

    PulsarMessageQueueEntry(byte[] id, @Nullable byte[] key, byte[] value, long timestamp) {
        this.commitId = null;
        this.id = requireNonNull(id, "id cannot be null"); // TODO: This is different to the constructor above. Should be the same!
                                                                    //       Can we store the actual ID?
        this.key = key;
        this.value = requireNonNull(value, "value cannot be null");
        this.timestamp = timestamp;
    }

    public static PulsarMessageQueueEntry fromMessage(Message<byte[]> message) {
        return new PulsarMessageQueueEntry(message);
    }

    @Nullable
    @Override
    public MessageId commitId() {
        return commitId;
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
                Arrays.equals(id, that.id) &&
                Arrays.equals(key, that.key) &&
                Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id, key, value, timestamp);
    }
}
