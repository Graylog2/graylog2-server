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
import org.apache.shiro.session.Session;

/**
 * Factory to create a JSON response for a given session. A plugin may provide a custom implementation, if additional
 * attributes are required in the response.
 */
public interface SessionResponseFactory {
    /**
     * Create a JSON response for the given session.
     */
    JsonNode forSession(Session session);
}
