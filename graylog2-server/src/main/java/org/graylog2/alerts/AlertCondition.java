/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 *
 */
package org.graylog2.alerts;

import com.google.common.collect.Maps;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.rest.resources.streams.alerts.requests.CreateConditionRequest;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class AlertCondition implements EmbeddedPersistable {

    private final String id;
    private final String type;
    private final DateTime createdAt;
    private final String creatorUserId;
    private final Map<String, Object> parameters;

    public AlertCondition(CreateConditionRequest ccr, DateTime createdAt) {
        this(UUID.randomUUID().toString(), ccr.type, createdAt, ccr.creatorUserId, ccr.parameters);
    }

    protected AlertCondition(String id, String type, DateTime createdAt, String creatorUserId, Map<String, Object> parameters) {
        this.id = id;
        this.type = type;
        this.createdAt = createdAt;
        this.creatorUserId = creatorUserId;
        this.parameters = parameters;
    }

    public static AlertCondition fromPersisted(Map<String, Object> fields) {
        return new AlertCondition(
                (String) fields.get("id"),
                (String) fields.get("type"),
                DateTime.parse((String) fields.get("created_at")),
                (String) fields.get("creator_user_id"),
                (Map<String, Object>) fields.get("parameters")
        );
    }

    @Override
    public Map<String, Object> getPersistedFields() {
        return new HashMap<String, Object>() {{
            put("id", id);
            put("type", type);
            put("creator_user_id", creatorUserId);
            put("created_at", Tools.getISO8601String(createdAt));
            put("parameters", parameters);
        }};
    }

    public String getId() {
        return id;
    }

    public Map<String, Object> asMap() {
        return new HashMap<String, Object>() {{
            put("id", id);
            put("type", type);
            put("creator_user_id", creatorUserId);
            put("created_at", Tools.getISO8601String(createdAt));
            put("parameters", parameters);
        }};
    }
}
