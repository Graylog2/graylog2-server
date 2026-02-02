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
package org.graylog2.opamp.config;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * ClusterConfig storage for OpAMP CA certificate references.
 * <p>
 * This record stores MongoDB ObjectId references to the CA certificates used for OpAMP:
 * <ul>
 *   <li>{@code enrollmentCaId} - the intermediate CA that signs agent CSRs during enrollment</li>
 *   <li>{@code tokenSigningCertId} - the cert that signs enrollment JWTs</li>
 * </ul>
 */
public record OpAmpCaConfig(
        @JsonProperty("enrollment_ca_id")
        String enrollmentCaId,

        @JsonProperty("token_signing_cert_id")
        String tokenSigningCertId
) {}
