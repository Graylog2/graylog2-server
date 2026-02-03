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
package org.graylog2.rest.models.system.sessions.responses;

import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.graylog2.rest.models.system.sessions.SessionUtils;
import org.graylog2.security.sessions.SessionAuthContext;

public class SessionResponseFactory {
    private SessionResponseFactory() {
    }

    public static SessionResponse forSession(Session session) {
        final var response = DefaultSessionResponse.builder()
                .validUntil(SessionUtils.getValidUntil(session))
                .sessionId(session.getId().toString())
                .userId(new Subject.Builder().sessionId(session.getId()).buildSubject().getPrincipal().toString())
                .username(String.valueOf(session.getAttribute(SessionUtils.USERNAME_SESSION_KEY)))
                .build();
        if (session.getAttribute(SessionUtils.AUTH_CONTEXT_SESSION_KEY) instanceof SessionAuthContext authContext) {
            return authContext.enrichSessionResponse(response);
        }
        return response;
    }
}
