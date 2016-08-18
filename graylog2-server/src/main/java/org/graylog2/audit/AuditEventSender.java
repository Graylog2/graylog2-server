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
package org.graylog2.audit;

import java.util.Map;

public interface AuditEventSender {
    void success(AuditActor actor, AuditEventType type);

    void success(AuditActor actor, AuditEventType type, Map<String, Object> context);

    void failure(AuditActor actor, AuditEventType type);

    void failure(AuditActor actor, AuditEventType type, Map<String, Object> context);

    // Some convenience default methods which an audit event type of "String".

    default void success(AuditActor actor, String type) {
        success(actor, AuditEventType.create(type));
    }

    default void success(AuditActor actor, String type, Map<String, Object> context) {
        success(actor, AuditEventType.create(type), context);
    }

    default void failure(AuditActor actor, String type) {
        failure(actor, AuditEventType.create(type));
    }

    default void failure(AuditActor actor, String type, Map<String, Object> context) {
        failure(actor, AuditEventType.create(type), context);
    }
}
