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
package org.graylog.security.pki;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.annotation.Nullable;
import org.graylog2.database.MongoEntity;
import org.graylog2.security.encryption.EncryptedValue;
import org.mongojack.Id;
import org.mongojack.ObjectId;

import java.time.Instant;
import java.util.List;

/**
 * MongoDB entity for storing certificates with their private keys and metadata.
 * Used for CA certificates and end-entity certificates in the certificate storage system.
 */
public record CertificateEntry(
        @ObjectId
        @Id
        @Nullable
        @JsonProperty(FIELD_ID)
        String id,

        @JsonProperty(FIELD_FINGERPRINT)
        String fingerprint,

        @JsonProperty(FIELD_PRIVATE_KEY)
        EncryptedValue privateKey,

        @JsonProperty(FIELD_CERTIFICATE)
        String certificate,

        @JsonProperty(FIELD_ISSUER_CHAIN)
        List<String> issuerChain,

        @Nullable
        @JsonProperty(FIELD_SUBJECT_DN)
        String subjectDn,

        @Nullable
        @JsonProperty(FIELD_ISSUER_DN)
        String issuerDn,

        @JsonProperty(FIELD_NOT_BEFORE)
        Instant notBefore,

        @JsonProperty(FIELD_NOT_AFTER)
        Instant notAfter,

        @JsonProperty(FIELD_CREATED_AT)
        Instant createdAt
) implements MongoEntity {

    public static final String FIELD_ID = "id";
    public static final String FIELD_FINGERPRINT = "fingerprint";
    public static final String FIELD_PRIVATE_KEY = "private_key";
    public static final String FIELD_CERTIFICATE = "certificate";
    public static final String FIELD_ISSUER_CHAIN = "issuer_chain";
    public static final String FIELD_SUBJECT_DN = "subject_dn";
    public static final String FIELD_ISSUER_DN = "issuer_dn";
    public static final String FIELD_NOT_BEFORE = "not_before";
    public static final String FIELD_NOT_AFTER = "not_after";
    public static final String FIELD_CREATED_AT = "created_at";

    /**
     * Creates a new CertificateEntry with the specified ID, preserving all other fields.
     *
     * @param newId the new ID to assign
     * @return a new CertificateEntry instance with the updated ID
     */
    public CertificateEntry withId(String newId) {
        return new CertificateEntry(
                newId,
                fingerprint,
                privateKey,
                certificate,
                issuerChain,
                subjectDn,
                issuerDn,
                notBefore,
                notAfter,
                createdAt
        );
    }
}
