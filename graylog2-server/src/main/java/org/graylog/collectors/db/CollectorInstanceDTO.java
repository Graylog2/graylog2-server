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
package org.graylog.collectors.db;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import jakarta.annotation.Nullable;
import org.graylog2.database.BuildableMongoEntity;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;

@AutoValue
@JsonDeserialize(builder = CollectorInstanceDTO.Builder.class)
public abstract class CollectorInstanceDTO implements BuildableMongoEntity<CollectorInstanceDTO, CollectorInstanceDTO.Builder> {

    @JsonProperty("instance_uid")
    public abstract String instanceUid();

    @JsonProperty("message_seq_num")
    public abstract long messageSeqNum();

    @JsonProperty("last_seen")
    public abstract Instant lastSeen();

    @JsonProperty("identifying_attributes")
    public abstract Optional<Map<String, Object>> identifyingAttributes();

    @JsonProperty("non_identifying_attributes")
    public abstract Optional<Map<String, Object>> nonIdentifyingAttributes();

    public static Builder builder() {
        return new AutoValue_CollectorInstanceDTO.Builder();
    }


    @AutoValue.Builder
    public abstract static class Builder implements BuildableMongoEntity.Builder<CollectorInstanceDTO, CollectorInstanceDTO.Builder> {

        @JsonCreator
        public static Builder create() {
            return new AutoValue_CollectorInstanceDTO.Builder();
        }

        @JsonProperty("instance_uid")
        public abstract Builder instanceUid(String instanceUid);

        @JsonProperty("message_seq_num")
        public abstract Builder messageSeqNum(long messageSeqNum);

        @JsonProperty("last_seen")
        public abstract Builder lastSeen(Instant lastSeen);

        @JsonProperty("identifying_attributes")
        public abstract Builder identifyingAttributes(@Nullable Map<String, Object> identifyingAttributes);

        @JsonProperty("non_identifying_attributes")
        public abstract Builder nonIdentifyingAttributes(@Nullable Map<String, Object> nonIdentifyingAttributes);

        public abstract CollectorInstanceDTO build();
    }
}
