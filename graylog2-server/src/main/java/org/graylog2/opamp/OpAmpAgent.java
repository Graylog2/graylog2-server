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
package org.graylog2.opamp;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import org.graylog2.database.MongoEntity;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.time.Instant;

/**
 * MongoDB entity for storing enrolled OpAMP agents.
 * Each record represents an agent that has successfully completed the enrollment process.
 */
public record OpAmpAgent(
        @ObjectId
        @Id
        @Nullable
        @JsonProperty(FIELD_ID)
        String id,

        @JsonProperty(FIELD_INSTANCE_UID)
        String instanceUid,

        @JsonProperty(FIELD_FLEET_ID)
        String fleetId,

        @JsonProperty(FIELD_CERTIFICATE_FINGERPRINT)
        String certificateFingerprint,

        @JsonProperty(FIELD_CERTIFICATE_PEM)
        String certificatePem,

        @ObjectId
        @JsonProperty(FIELD_ISSUING_CA_ID)
        String issuingCaId,

        @JsonProperty(FIELD_ENROLLED_AT)
        Instant enrolledAt
) implements MongoEntity {

    public static final String FIELD_ID = "id";
    public static final String FIELD_INSTANCE_UID = "instance_uid";
    public static final String FIELD_FLEET_ID = "fleet_id";
    public static final String FIELD_CERTIFICATE_FINGERPRINT = "certificate_fingerprint";
    public static final String FIELD_CERTIFICATE_PEM = "certificate_pem";
    public static final String FIELD_ISSUING_CA_ID = "issuing_ca_id";
    public static final String FIELD_ENROLLED_AT = "enrolled_at";

    /**
     * Creates a new OpAmpAgent with the specified ID, preserving all other fields.
     *
     * @param newId the new ID to assign
     * @return a new OpAmpAgent instance with the updated ID
     */
    public OpAmpAgent withId(String newId) {
        return new OpAmpAgent(
                newId,
                instanceUid,
                fleetId,
                certificateFingerprint,
                certificatePem,
                issuingCaId,
                enrolledAt
        );
    }
}
