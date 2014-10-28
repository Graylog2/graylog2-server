/**
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
 */
package org.graylog2.alerts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.ImmutableMap;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;

import java.util.Map;
import java.util.UUID;

public abstract class AbstractAlertCondition implements EmbeddedPersistable, AlertCondition {

    public enum Type {
        MESSAGE_COUNT,
        FIELD_VALUE,
        DUMMY
    }

    protected final String id;
    protected final Stream stream;
    protected final Type type;
    protected final DateTime createdAt;
    protected final String creatorUserId;
    protected final int grace;

    private final Map<String, Object> parameters;

    protected AbstractAlertCondition(Stream stream, String id, Type type, DateTime createdAt, String creatorUserId, Map<String, Object> parameters) {
        if (id == null) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }

        this.stream = stream;
        this.type = type;
        this.createdAt = createdAt;
        this.creatorUserId = creatorUserId;
        this.parameters = parameters;

        if (this.parameters.containsKey("grace")) {
            this.grace = (Integer) this.parameters.get("grace");
        } else {
            this.grace = 0;
        }

    }

    protected abstract CheckResult runCheck();

    @Override
    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    @Override
    public String getTypeString() {
        return type.toString();
    }

    @Override
    public DateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String getCreatorUserId() {
        return creatorUserId;
    }

    @JsonIgnore
    public Stream getStream() {
        return stream;
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public Integer getBacklog() {
        Object rawParameter = getParameters().get("backlog");
        if (rawParameter != null && rawParameter instanceof Number) {
            return (Integer) rawParameter;
        } else {
            return 0;
        }
    }

    @Override
    public String toString() {
        return id + ":" + type + "={" + getDescription() + "}" + ", stream:={" + stream + "}";
    }

    @Override
    @JsonIgnore
    public Map<String, Object> getPersistedFields() {
        return ImmutableMap.<String, Object>builder()
                .put("id", id)
                .put("type", type.toString().toLowerCase())
                .put("creator_user_id", creatorUserId)
                .put("created_at", Tools.getISO8601String(createdAt))
                .put("parameters", parameters)
                .build();
    }

    @Override
    public int getGrace() {
        return grace;
    }

    public static class NoSuchAlertConditionTypeException extends Exception {
        public NoSuchAlertConditionTypeException(String msg) {
            super(msg);
        }
    }

    public static class CheckResult implements AlertCondition.CheckResult {

        private final boolean isTriggered;
        private final String resultDescription;
        private final AlertCondition triggeredCondition;
        private final DateTime triggeredAt;

        public CheckResult(boolean isTriggered, AlertCondition triggeredCondition, String resultDescription, DateTime triggeredAt) {
            this.isTriggered = isTriggered;
            this.resultDescription = resultDescription;
            this.triggeredCondition = triggeredCondition;
            this.triggeredAt = triggeredAt;
        }

        public CheckResult(boolean isTriggered) {
            this(false, null, null, null);
            if (isTriggered)
                throw new RuntimeException("Boolean only constructor should only be called if CheckResult is not triggered!");
        }

        public boolean isTriggered() {
            return isTriggered;
        }

        public String getResultDescription() {
            return resultDescription;
        }

        public AlertCondition getTriggeredCondition() {
            return triggeredCondition;
        }

        public DateTime getTriggeredAt() {
            return triggeredAt;
        }
    }

}
