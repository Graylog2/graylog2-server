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
package org.graylog2.rest.models.system.sessions;

import org.apache.shiro.session.Session;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Years;

import java.util.Date;

public class SessionUtils {
    public static final String USERNAME_SESSION_KEY = "username";
    public static final String AUTH_CONTEXT_SESSION_KEY = "auth_context";

    private SessionUtils() {
    }

    /**
     * Calculates the expiration date of a session.
     * If the session timeout is negative, returns a date 10 years in the future.
     * Otherwise, returns the last access time plus the session timeout.
     *
     * @param session the session to evaluate
     * @return the expiration date of the session
     */
    public static Date getValidUntil(Session session) {
        if (session.getTimeout() < 0) { // means "session never expires", which is not possible in cookie-based auth
            return new DateTime(DateTimeZone.UTC).plus(Years.years(10)).toDate(); // careful, later we convert the date to seconds as int and it may overflow for too big values
        } else {
            return new DateTime(session.getLastAccessTime(), DateTimeZone.UTC).plus(session.getTimeout()).toDate();
        }
    }
}
