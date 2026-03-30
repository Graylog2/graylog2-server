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
package org.graylog.collectors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.graylog2.jackson.MongoInstantDeserializer;
import org.graylog2.jackson.MongoInstantSerializer;
import org.graylog2.security.encryption.EncryptedValue;

import java.time.Instant;

/**
 * Holds a cryptographic key pair used to sign collector enrollment tokens.
 * The private key is stored as an {@link EncryptedValue} to ensure it is encrypted at rest in MongoDB.
 *
 * @param privateKey  the encrypted private key used for signing tokens
 * @param fingerprint a unique identifier derived from the public key, used to look up the corresponding key pair
 * @param createdAt   the timestamp when this signing key was generated
 */
public record TokenSigningKey(
        @JsonProperty("private_key")
        EncryptedValue privateKey,
        @JsonProperty("public_key")
        String publicKey,
        @JsonProperty("fingerprint") String fingerprint,
        @JsonSerialize(using = MongoInstantSerializer.class)
        @JsonDeserialize(using = MongoInstantDeserializer.class)
        @JsonProperty("created_at")
        Instant createdAt
) {
}
