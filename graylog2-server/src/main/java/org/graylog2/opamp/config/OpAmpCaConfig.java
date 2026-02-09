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
 *   <li>{@code opampCaId} - the intermediate CA used for OpAMP (signs agent CSRs and issues OTLP server certs)</li>
 *   <li>{@code tokenSigningCertId} - the cert that signs enrollment JWTs</li>
 *   <li>{@code otlpServerCertId} - the TLS server certificate used by the OTLP ingest endpoint</li>
 * </ul>
 */
public record OpAmpCaConfig(
        @JsonProperty("opamp_ca_id")
        String opampCaId,

        @JsonProperty("token_signing_cert_id")
        String tokenSigningCertId,

        @JsonProperty("otlp_server_cert_id")
        String otlpServerCertId
) {}
