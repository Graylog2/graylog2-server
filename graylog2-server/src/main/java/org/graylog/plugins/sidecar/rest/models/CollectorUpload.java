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
package org.graylog.plugins.sidecar.rest.models;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.joda.time.DateTime;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import javax.annotation.Nullable;

@AutoValue
@JsonAutoDetect
public abstract class CollectorUpload {
    private static final String FIELD_ID = "id";
    private static final String FIELD_COLLECTOR_ID = "collector_id";
    private static final String FIELD_NODE_ID = "node_id";
    private static final String FIELD_COLLECTOR_NAME = "collector_name";
    private static final String FIELD_RENDERED_CONFIGURATION = "rendered_configuration";
    private static final String FIELD_CREATED = "created";

    @JsonProperty(FIELD_ID)
    @Id
    @ObjectId
    @Nullable
    public abstract String id();

    @JsonProperty(FIELD_COLLECTOR_ID)
    public abstract String collectorId();

    @JsonProperty(FIELD_NODE_ID)
    public abstract String nodeId();

    @JsonProperty(FIELD_COLLECTOR_NAME)
    public abstract String collectorName();

    @JsonProperty(FIELD_RENDERED_CONFIGURATION)
    public abstract String renderedConfiguration();

    @JsonProperty(FIELD_CREATED)
    @Nullable
    public abstract DateTime created();

    public static Builder builder() {
        return new AutoValue_CollectorUpload.Builder();
    }

    public abstract Builder toBuilder();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder id(String id);
        public abstract Builder collectorId(String collectorId);
        public abstract Builder nodeId(String nodeId);
        public abstract Builder collectorName(String collectorName);
        public abstract Builder renderedConfiguration(String renderedConfiguration);
        public abstract Builder created(DateTime created);
        public abstract CollectorUpload build();
    }

    @JsonCreator
    public static CollectorUpload create(@JsonProperty(FIELD_ID) String id,
                                         @JsonProperty(FIELD_COLLECTOR_ID) String collectorId,
                                         @JsonProperty(FIELD_NODE_ID) String nodeId,
                                         @JsonProperty(FIELD_COLLECTOR_NAME) String collectorName,
                                         @JsonProperty(FIELD_RENDERED_CONFIGURATION) String renderedConfiguration,
                                         @JsonProperty(FIELD_CREATED) @Nullable DateTime created) {
        return builder()
                .id(id)
                .collectorId(collectorId)
                .nodeId(nodeId)
                .collectorName(collectorName)
                .renderedConfiguration(renderedConfiguration)
                .created(created)
                .build();
    }
}
