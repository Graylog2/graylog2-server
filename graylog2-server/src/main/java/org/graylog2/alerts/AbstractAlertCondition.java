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
package org.graylog2.alerts;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.configuration.fields.BooleanField;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.NumberField;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkArgument;

public abstract class AbstractAlertCondition implements EmbeddedPersistable, AlertCondition {
    protected static final String CK_QUERY = "query";
    protected static final String CK_QUERY_DEFAULT_VALUE = "*";

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
    protected final int backlog;
    protected final boolean repeatNotifications;
    protected final String title;

    private Map<String, Object> parameters;

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

        this.grace = Tools.getNumber(this.parameters.get("grace"), 0).intValue();
        this.backlog = Tools.getNumber(this.parameters.get("backlog"), 0).intValue();
        this.repeatNotifications = (boolean) this.parameters.getOrDefault("repeat_notifications", false);
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getType() {
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

    protected void setParameters(Map<String, Object> parameters) {
        this.parameters = ImmutableMap.copyOf(parameters);
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public Integer getBacklog() {
        return backlog;
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
                .put("type", type)
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

    @Override
    public boolean shouldRepeatNotifications() {
        return repeatNotifications;
    }

    /**
     * Combines the given stream ID and query string into a single filter string.
     *
     * @param streamId the stream ID
     * @param query    the query string (might be null or empty)
     * @return the combined filter string
     */
    protected String buildQueryFilter(String streamId, String query) {
        checkArgument(streamId != null, "streamId parameter cannot be null");

        final String trimmedStreamId = streamId.trim();

        checkArgument(!trimmedStreamId.isEmpty(), "streamId parameter cannot be empty");

        final StringBuilder builder = new StringBuilder().append("streams:").append(trimmedStreamId);

        if (query != null) {
            final String trimmedQuery = query.trim();
            if (!trimmedQuery.isEmpty() && !"*".equals(trimmedQuery)) {
                builder.append(" AND (").append(trimmedQuery).append(")");
            }
        }

        return builder.toString();
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
        public NegativeCheckResult() {
            super(false, null, null, null, null);
        }
    }

    public static List<ConfigurationField> getDefaultConfigurationFields() {
        return Lists.newArrayList(
            // The query field needs to be optional for backwards compatibility
            new TextField(CK_QUERY, "Search Query", CK_QUERY_DEFAULT_VALUE, "Query string that should be used to filter messages in the stream", ConfigurationField.Optional.OPTIONAL),
            new NumberField("grace", "Grace Period", 0, "Number of minutes to wait after an alert is resolved, to trigger another alert", ConfigurationField.Optional.NOT_OPTIONAL),
            new NumberField("backlog", "Message Backlog", 0, "The number of messages to be included in alert notifications", ConfigurationField.Optional.NOT_OPTIONAL),
            new BooleanField("repeat_notifications", "Repeat notifications", false, "Check this box to send notifications every time the alert condition is evaluated and satisfied regardless of its state.")
        );
    }
}
