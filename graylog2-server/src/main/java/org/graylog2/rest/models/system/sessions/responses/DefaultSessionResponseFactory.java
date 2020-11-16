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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.Subject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;
import java.util.Date;

/**
 * Creates a session response which contains the common attributes of the session.
 */
public class DefaultSessionResponseFactory implements SessionResponseFactory {

    protected final ObjectMapper objectMapper;

    @Inject
    public DefaultSessionResponseFactory(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode forSession(Session session) {
        Date validUntil = getValidUntil(session);
        String id = session.getId().toString();
        String userId = getSubjectFromSession(session).getPrincipal().toString();
        String username = String.valueOf(session.getAttribute("username"));
        return toJsonNode(DefaultSessionResponse.create(validUntil, id, username, userId));
    }

    protected Date getValidUntil(Session session) {
        return new DateTime(session.getLastAccessTime(), DateTimeZone.UTC).plus(session.getTimeout()).toDate();
    }

    protected Subject getSubjectFromSession(Session session) {
        return new Subject.Builder().sessionId(session.getId())
                .buildSubject();
    }

    protected JsonNode toJsonNode(Object object) {
        return objectMapper.valueToTree(object);
    }
}
