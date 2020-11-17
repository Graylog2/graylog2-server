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
package org.graylog2.shared.security;

import org.apache.shiro.authc.UsernamePasswordToken;
import org.graylog2.audit.AuditActor;

import javax.annotation.Nonnull;
import java.util.Objects;

/**
 * Audit actor is always the username.
 */
public class ActorAwareUsernamePasswordToken extends UsernamePasswordToken implements ActorAwareAuthenticationToken {

    public ActorAwareUsernamePasswordToken(@Nonnull String username, @Nonnull String password) {
        super(username, password);
        Objects.requireNonNull(username);
        Objects.requireNonNull(password);
    }

    @Override
    public AuditActor getActor() {
         return AuditActor.user(getUsername());
    }
}
