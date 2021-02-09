/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.shared.messageq.kafka;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.TopicPartition;
import org.graylog2.shared.messageq.MessageQueue;

import javax.annotation.Nullable;
import java.util.Arrays;

import static java.util.Objects.requireNonNull;

public class KafkaMessageQueueEntry implements MessageQueue.Entry {
    private final byte[] value;
    private final long timestamp;
    private final CommitId commitId;

    public KafkaMessageQueueEntry(ConsumerRecord<String, byte[]> message) {
        this.commitId = CommitId.fromRecord(message);
        //bthis.id = commitId.toByteArray();
        //this.key = message.getKey().getBytes(UTF_8);
        this.value = requireNonNull(message.value(), "value cannot be null");
        this.timestamp = message.timestamp();
    }


    @Override
    public CommitId commitId() {
        return commitId;
    }

    @Override
    public byte[] id() {
        return new byte[0];
    }

    @Nullable
    @Override
    public byte[] key() {
        return new byte[0];
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
                .add("value.len", value.length)
                .add("timestamp", timestamp)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaMessageQueueEntry that = (KafkaMessageQueueEntry) o;
        return timestamp == that.timestamp &&
                Arrays.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(Arrays.hashCode(value), timestamp);
    }

    static class CommitId {
        private final TopicPartition topicPartition;
        private final long offset;

        public CommitId(TopicPartition topicPartition, long offset) {
            this.topicPartition = topicPartition;
            this.offset = offset;
        }

        public TopicPartition getTopicPartition() {
            return topicPartition;
        }

        public long getOffset() {
            return offset;
        }

        static CommitId fromRecord(ConsumerRecord<String, byte[]> record) {
            return new CommitId(new TopicPartition(record.topic(), record.partition()), record.offset());
        }
    }
}
