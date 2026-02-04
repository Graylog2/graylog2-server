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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import opamp.proto.Opamp.AgentToServer;
import opamp.proto.Opamp.ServerToAgent;
import org.graylog2.opamp.enrollment.EnrollmentTokenService;
import org.graylog2.opamp.transport.OpAmpAuthContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Optional;
import java.util.UUID;

@Singleton
public class OpAmpService {
    private static final Logger LOG = LoggerFactory.getLogger(OpAmpService.class);

    private final EnrollmentTokenService enrollmentTokenService;

    @Inject
    public OpAmpService(EnrollmentTokenService enrollmentTokenService) {
        this.enrollmentTokenService = enrollmentTokenService;
    }

    public Optional<OpAmpAuthContext> authenticate(String authHeader, URI effectiveExternalUri) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        final String token = authHeader.substring(7);
        return enrollmentTokenService.validateToken(token, effectiveExternalUri)
                .map(e -> e);  // Upcast Enrollment to OpAmpAuthContext
    }

    public ServerToAgent handleMessage(AgentToServer message, OpAmpAuthContext authContext) {
        final UUID instanceUid = bytesToUuid(message.getInstanceUid().toByteArray());
        switch (authContext) {
            case OpAmpAuthContext.Enrollment e -> LOG.info("Received OpAMP enrollment from agent {} for fleet {}",
                    instanceUid, e.fleetId());
        }

        // Skeleton - just acknowledge
        return ServerToAgent.newBuilder()
                .setInstanceUid(message.getInstanceUid())
                .build();
    }

    private UUID bytesToUuid(byte[] bytes) {
        final ByteBuffer bb = ByteBuffer.wrap(bytes);
        return new UUID(bb.getLong(), bb.getLong());
    }
}
