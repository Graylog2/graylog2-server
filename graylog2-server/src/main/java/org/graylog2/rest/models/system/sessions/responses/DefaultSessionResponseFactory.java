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
import org.graylog2.security.sessions.SessionDTO;

import java.util.Date;

/**
 * Creates a session response which contains the common attributes of the session.
 */
public class DefaultSessionResponseFactory implements SessionResponseFactory {

    @Override
    public SessionResponse forSession(Session session) {
        final Date validUntil = SessionUtils.getValidUntil(session);
        final String id = session.getId().toString();
        final String userId = getSubjectFromSession(session).getPrincipal().toString();
        final String username = String.valueOf(session.getAttribute(SessionDTO.USERNAME_SESSION_KEY));
        return DefaultSessionResponse.create(validUntil, id, username, userId);
    }

    protected Date getValidUntil(Session session) {
        return SessionUtils.getValidUntil(session);
    }

    protected Subject getSubjectFromSession(Session session) {
        return new Subject.Builder().sessionId(session.getId())
                .buildSubject();
    }
}
