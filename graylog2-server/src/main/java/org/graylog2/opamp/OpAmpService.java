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
import org.apache.commons.lang3.Strings;
import org.graylog2.opamp.transport.OpAmpAuthContext;
import org.graylog2.security.AccessTokenService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

@Singleton
public class OpAmpService {
    private static final Logger LOG = LoggerFactory.getLogger(OpAmpService.class);

    private final AccessTokenService accessTokenService;

    @Inject
    public OpAmpService(AccessTokenService accessTokenService) {
        this.accessTokenService = accessTokenService;
    }

    public Optional<OpAmpAuthContext> authenticate(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return Optional.empty();
        }
        final var token = Strings.CS.removeStart(authHeader, "Bearer ");

        // TODO: permission check, populate context with token info
        if (accessTokenService.load(token) != null) {
            return Optional.of(new OpAmpAuthContext(true));
        }
        return Optional.empty();
    }

    public ServerToAgent handleMessage(AgentToServer message) {
        LOG.info("Received OpAMP message from agent: {}", message);

        // Skeleton - just acknowledge
        return ServerToAgent.newBuilder()
                .setInstanceUid(message.getInstanceUid())
                .build();
    }
}
