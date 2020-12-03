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
import org.graylog2.shared.messageq.MessageQueue;

import javax.annotation.Nullable;

import static java.util.Objects.requireNonNull;

public class KafkaMessageQueueEntry implements MessageQueue.Entry {
    private final byte[] value;
    private final long offset;

    KafkaMessageQueueEntry(byte[] value, long offset) {
        this.value = requireNonNull(value, "value cannot be null");
        this.offset = offset;
    }

    @Nullable
    @Override
    public Long commitId() {
        return offset;
    }

    @Override
    public byte[] id() {
        return null;
    }

    @Nullable
    @Override
    public byte[] key() {
        return null;
    }

    @Override
    public byte[] value() {
        return value;
    }

    @Override
    public long timestamp() {
        return 0;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("commitId", offset)
                .add("value.len", value.length)
                .toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        KafkaMessageQueueEntry that = (KafkaMessageQueueEntry) o;
        return
                Objects.equal(commitId(), that.commitId()) &&
                Objects.equal(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(offset, value);
    }
}
