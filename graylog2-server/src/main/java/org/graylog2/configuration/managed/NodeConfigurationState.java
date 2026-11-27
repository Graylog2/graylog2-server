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
package org.graylog2.configuration.managed;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import jakarta.annotation.Nullable;
import org.graylog2.database.MongoEntity;
import org.graylog2.jackson.MongoInstantDeserializer;
import org.graylog2.jackson.MongoInstantSerializer;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.time.Instant;

/**
 * When a node last picked up its stored configuration. A stored value which has been changed after
 * {@link #appliedAt} isn't in use on that node yet, which is how the need for a restart is derived without the node
 * reporting every value it runs.
 * <p>
 * Both timestamps are taken from the clock of the database, like the change time of the values they are compared
 * with, so differing clocks of the nodes don't matter.
 *
 * @param loadedAt  When the node last loaded its stored values to start its process with them
 * @param appliedAt The {@link #loadedAt} of the configuration the running process has been started with
 * @param liveSettingsError Why the node couldn't apply the stored settings which take effect without a restart the
 *                          last time it tried, {@code null} if it succeeded
 */
public record NodeConfigurationState(
        @ObjectId @Id @Nullable @JsonProperty(FIELD_ID) String id,
        @JsonProperty(FIELD_NODE_ID) String nodeId,
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_LOADED_AT) @Nullable Instant loadedAt,
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty(FIELD_APPLIED_AT) @Nullable Instant appliedAt,
        @JsonProperty(FIELD_LIVE_SETTINGS_ERROR) @Nullable String liveSettingsError
) implements MongoEntity {
    public static final String FIELD_NODE_ID = "node_id";
    public static final String FIELD_LOADED_AT = "loaded_at";
    public static final String FIELD_APPLIED_AT = "applied_at";
    public static final String FIELD_LIVE_SETTINGS_ERROR = "live_settings_error";
}
