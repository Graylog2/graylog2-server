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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoBuilder;
import jakarta.annotation.Nullable;
import org.graylog2.database.MongoEntity;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.time.Instant;

public record EnrollmentTokenDTO(
        @ObjectId @Id @Nullable @JsonProperty(MongoEntity.FIELD_ID) String id,
        @JsonProperty(FIELD_JTI) String jti,
        @JsonProperty(FIELD_KID) String kid,
        @JsonProperty(FIELD_FLEET_ID) String fleetId,
        @JsonProperty(FIELD_CREATED_BY) EnrollmentTokenCreator createdBy,
        @JsonProperty(FIELD_CREATED_AT) Instant createdAt,
        @Nullable @JsonProperty(FIELD_EXPIRES_AT) Instant expiresAt,
        @JsonProperty(FIELD_USAGE_COUNT) int usageCount,
        @Nullable @JsonProperty(FIELD_LAST_USED_AT) Instant lastUsedAt
) implements MongoEntity {
    public static final String FIELD_JTI = "jti";
    public static final String FIELD_KID = "kid";
    public static final String FIELD_FLEET_ID = "fleet_id";
    public static final String FIELD_CREATED_BY = "created_by";
    public static final String FIELD_CREATED_AT = "created_at";
    public static final String FIELD_EXPIRES_AT = "expires_at";
    public static final String FIELD_USAGE_COUNT = "usage_count";
    public static final String FIELD_LAST_USED_AT = "last_used_at";

    public static Builder builder() {
        return new AutoBuilder_EnrollmentTokenDTO_Builder().usageCount(0);
    }

    @AutoBuilder
    public interface Builder {
        Builder id(@Nullable String id);

        Builder jti(String jti);

        Builder kid(String kid);

        Builder fleetId(String fleetId);

        Builder createdBy(EnrollmentTokenCreator createdBy);

        Builder createdAt(Instant createdAt);

        Builder expiresAt(@Nullable Instant expiresAt);

        Builder usageCount(int usageCount);

        Builder lastUsedAt(@Nullable Instant lastUsedAt);

        EnrollmentTokenDTO build();
    }
}
