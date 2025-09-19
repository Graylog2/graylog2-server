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
package org.graylog2.cluster;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import jakarta.validation.constraints.NotEmpty;
import org.graylog2.database.MongoEntity;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
public abstract class ClusterConfig implements MongoEntity {
    @JsonProperty("type")
    @Nullable
    public abstract String type();

    @JsonProperty("payload")
    @Nullable
    public abstract Object payload();

    @JsonProperty("last_updated")
    @Nullable
    public abstract DateTime lastUpdated();

    @JsonProperty("last_updated_by")
    @Nullable
    public abstract String lastUpdatedBy();

    @JsonCreator
    public static ClusterConfig create(@JsonProperty(FIELD_ID) @Id @ObjectId @Nullable String id,
                                       @JsonProperty("type") @Nullable String type,
                                       @JsonProperty("payload") @Nullable Object payload,
                                       @JsonProperty("last_updated") @Nullable DateTime lastUpdated,
                                       @JsonProperty("last_updated_by") @Nullable String lastUpdatedBy) {
        return new AutoValue_ClusterConfig(id, type, payload, lastUpdated, lastUpdatedBy);
    }

    public static ClusterConfig create(@NotEmpty String type,
                                       @NotEmpty Object payload,
                                       @NotEmpty String nodeId) {
        return create(null, type, payload, DateTime.now(DateTimeZone.UTC), nodeId);
    }
}
