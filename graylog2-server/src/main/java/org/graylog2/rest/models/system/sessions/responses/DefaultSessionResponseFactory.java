/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
        String username = getSubjectFromSession(session).getPrincipal().toString();
        return toJsonNode(DefaultSessionResponse.create(validUntil, id, username));
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
