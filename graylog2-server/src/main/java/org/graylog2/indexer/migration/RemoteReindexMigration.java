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
package org.graylog2.indexer.migration;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.indexer.datanode.RemoteReindexingMigrationAdapter.Status;

import javax.annotation.Nullable;
import java.util.Collection;

@AutoValue
@JsonDeserialize(builder = RemoteReindexMigration.Builder.class)
@WithBeanGetter
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class RemoteReindexMigration {
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_INDICES = "indices";
    private static final String FIELD_ERROR = "error";

    @JsonProperty(FIELD_STATUS)
    public abstract Status status();

    @JsonProperty(FIELD_INDICES)
    public abstract Collection<RemoteReindexIndex> indices();

    @JsonProperty(FIELD_ERROR)
    @Nullable
    public abstract String error();

    public static Builder builder() {
        return Builder.create();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public static abstract class Builder {
        @JsonProperty(FIELD_STATUS)
        public abstract Builder status(Status status);

        @JsonProperty(FIELD_STATUS)
        public abstract Builder indices(Collection<RemoteReindexIndex> indices);

        @JsonProperty(FIELD_ERROR)
        @Nullable
        public abstract Builder error(String error);

        @JsonCreator
        public static Builder create() {
            return new AutoValue_RemoteReindexMigration.Builder();
        }

        public abstract RemoteReindexMigration build();
    }
}
