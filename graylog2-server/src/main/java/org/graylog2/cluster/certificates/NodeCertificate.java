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
package org.graylog2.cluster.certificates;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.graylog2.security.encryption.EncryptedValue;

public record NodeCertificate(@JsonProperty(FIELD_NODEID) String nodeId,
                              @JsonProperty(FIELD_ENCR_CERTIFICATE) EncryptedValue encryptedCertificate) {

    public static final String FIELD_NODEID = "node_id";
    public static final String FIELD_ENCR_CERTIFICATE = "encrypted_certificate_keystore";

}
