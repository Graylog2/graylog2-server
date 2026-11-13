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
package org.graylog2.presence;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import org.graylog2.database.MongoEntity;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.util.Date;

/**
 * One browser tab of a user viewing a resource, last seen at {@link #lastSeen()}.
 */
public record PresenceEntry(@JsonProperty(FIELD_ID) @Nullable @Id @ObjectId String id,
                            @JsonProperty(FIELD_RESOURCE) String resource,
                            @JsonProperty(FIELD_USER_ID) String userId,
                            @JsonProperty(FIELD_CLIENT_ID) String clientId,
                            @JsonProperty(FIELD_LAST_SEEN) Date lastSeen) implements MongoEntity {
    public static final String FIELD_ID = "id";
    public static final String FIELD_RESOURCE = "resource";
    public static final String FIELD_USER_ID = "user_id";
    public static final String FIELD_CLIENT_ID = "client_id";
    public static final String FIELD_LAST_SEEN = "last_seen";
}
