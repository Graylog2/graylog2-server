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

import jakarta.inject.Inject;
import org.graylog.collectors.opamp.auth.EnrollmentTokenService;

public class CollectorsInitializer {
    private final CollectorCaService caService;
    private final EnrollmentTokenService enrollmentTokenService;

    @Inject
    public CollectorsInitializer(CollectorCaService caService,
                                 EnrollmentTokenService enrollmentTokenService) {
        this.caService = caService;
        this.enrollmentTokenService = enrollmentTokenService;
    }

    /**
     * One-time bootstrap of the CA hierarchy and the enrollment token signing key.
     */
    public CollectorsConfig initialize(CollectorsConfig config) {
        final var caHierarchy = caService.initializeCa();
        final var tokenSigningKey = createTokenSigningKey();

        return config.toBuilder()
                .caCertId(caHierarchy.caCert().id())
                .signingCertId(caHierarchy.signingCert().id())
                .tokenSigningKey(tokenSigningKey)
                .otlpServerCertId(caHierarchy.otlpServerCert().id())
                .build();
    }

    private TokenSigningKey createTokenSigningKey() {
        try {
            return enrollmentTokenService.createTokenSigningKey();
        } catch (Exception e) {
            throw new IllegalStateException("Could not create token signing key", e);
        }
    }
}
