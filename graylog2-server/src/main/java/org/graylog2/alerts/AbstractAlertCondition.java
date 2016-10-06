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
package org.graylog2.alerts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public abstract class AbstractAlertCondition implements EmbeddedPersistable, AlertCondition {

    public enum Type {
        MESSAGE_COUNT,
        FIELD_VALUE,
        FIELD_CONTENT_VALUE,
        DUMMY;

        @JsonValue
        @Override
        public String toString() {
            return super.toString().toLowerCase(Locale.ENGLISH);
        }
    }

    protected final String id;
    protected final Stream stream;
    protected final String type;
    protected final DateTime createdAt;
    protected final String creatorUserId;
    protected final int grace;
    protected final String title;

    private final Map<String, Object> parameters;

    protected AbstractAlertCondition(Stream stream, String id, String type, DateTime createdAt, String creatorUserId, Map<String, Object> parameters, String title) {
        this.title = title;
        if (id == null) {
            this.id = UUID.randomUUID().toString();
        } else {
            this.id = id;
        }

        this.stream = stream;
        this.type = type;
        this.createdAt = createdAt;
        this.creatorUserId = creatorUserId;
        this.parameters = ImmutableMap.copyOf(parameters);

        this.grace = getNumber(this.parameters.get("grace")).orElse(0).intValue();
    }

    @Override
    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    @Override
    public String getTypeString() {
        return type;
    }

    @Override
    public String getTitle() {
        return title;
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
    @Override
    public Stream getStream() {
        return stream;
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public Integer getBacklog() {
        return getNumber(getParameters().get("backlog")).orElse(0).intValue();
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
                .put("type", type.toLowerCase(Locale.ENGLISH))
                .put("creator_user_id", creatorUserId)
                .put("created_at", Tools.getISO8601String(createdAt))
                .put("parameters", parameters)
                .put("title", title)
                .build();
    }

    @Override
    public int getGrace() {
        return grace;
    }

    public static class CheckResult implements AlertCondition.CheckResult {

        private final boolean isTriggered;
        private final String resultDescription;
        private final AlertCondition triggeredCondition;
        private final DateTime triggeredAt;
        private final ArrayList<MessageSummary> summaries = Lists.newArrayList();

        public CheckResult(boolean isTriggered,
                           AlertCondition triggeredCondition,
                           String resultDescription,
                           DateTime triggeredAt,
                           List<MessageSummary> summaries) {
            this.isTriggered = isTriggered;
            this.resultDescription = resultDescription;
            this.triggeredCondition = triggeredCondition;
            this.triggeredAt = triggeredAt;
            if (summaries != null) {
                this.summaries.addAll(summaries);
            }
        }

        @Override
        public boolean isTriggered() {
            return isTriggered;
        }

        @Override
        public String getResultDescription() {
            return resultDescription;
        }

        @Override
        public AlertCondition getTriggeredCondition() {
            return triggeredCondition;
        }

        @Override
        public DateTime getTriggeredAt() {
            return triggeredAt;
        }

        @Override
        public List<MessageSummary> getMatchingMessages() {
            return summaries;
        }
    }

    public static class NegativeCheckResult extends CheckResult {
        public NegativeCheckResult(AlertCondition alertCondition) {
            super(false, alertCondition, null, null, null);
        }
    }

    protected Optional<Number> getNumber(Object o) {
        if (o instanceof Number) {
            return Optional.of((Number)o);
        }

        try {
            return Optional.of(Double.valueOf(String.valueOf(o)));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    public static List<ConfigurationField> getDefaultConfigurationFields() {
        return Lists.newArrayList(
            new NumberField("grace", "Grace Period", 0, "Time span in seconds defining how long alerting is paused after alert is triggered", ConfigurationField.Optional.NOT_OPTIONAL),
            new NumberField("backlog", "Message Backlog", 0, "The number of messages to be included in alert notification", ConfigurationField.Optional.NOT_OPTIONAL)
        );
    }
}
