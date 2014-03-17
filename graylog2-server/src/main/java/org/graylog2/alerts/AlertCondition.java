/*
 * Copyright 2014 TORCH GmbH
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
 */
package org.graylog2.alerts;

import org.elasticsearch.search.SearchHits;
import org.graylog2.Core;
import org.graylog2.alerts.types.FieldValueAlertCondition;
import org.graylog2.alerts.types.MessageCountAlertCondition;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.resources.streams.alerts.requests.CreateConditionRequest;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public abstract class AlertCondition implements EmbeddedPersistable {

    private static final Logger LOG = LoggerFactory.getLogger(AlertCondition.class);

    public enum Type {
        MESSAGE_COUNT,
        FIELD_VALUE
    }

    protected final String id;
    protected final Stream stream;
    protected final Type type;
    protected final DateTime createdAt;
    protected final String creatorUserId;
    protected final int grace;
    protected final Core core;

    private final Map<String, Object> parameters;

    protected AlertCondition(Core core, Stream stream, String id, Type type, DateTime createdAt, String creatorUserId, Map<String, Object> parameters) {
        if(id == null) {
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

        this.core = core;
    }

    public abstract String getDescription();
    protected abstract CheckResult runCheck();
    public abstract List<ResultMessage> getSearchHits();

    public static AlertCondition fromRequest(CreateConditionRequest ccr, Stream stream, Core core) throws NoSuchAlertConditionTypeException {
        Type type;
        try {
            type = Type.valueOf(ccr.type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NoSuchAlertConditionTypeException("No such alert condition type: [" + ccr.type + "]");
        }

        Map<String, Object> parameters = ccr.parameters;

        switch(type) {
            case MESSAGE_COUNT:
                return new MessageCountAlertCondition(
                        core,
                        stream,
                        null,
                        Tools.iso8601(),
                        ccr.creatorUserId,
                        parameters
                );
            case FIELD_VALUE:
                return new FieldValueAlertCondition(
                        core,
                        stream,
                        null,
                        Tools.iso8601(),
                        ccr.creatorUserId,
                        parameters
                );
        }

        throw new NoSuchAlertConditionTypeException("Unhandled alert condition type: " + type);
    }

    public static AlertCondition fromPersisted(Map<String, Object> fields, Stream stream, Core core) throws NoSuchAlertConditionTypeException {
        Type type;
        try {
            type = Type.valueOf(((String) fields.get("type")).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NoSuchAlertConditionTypeException("No such alert condition type: [" + fields.get("type") + "]");
        }

        switch(type) {
            case MESSAGE_COUNT:
                return new MessageCountAlertCondition(
                        core,
                        stream,
                        (String) fields.get("id"),
                        DateTime.parse((String) fields.get("created_at")),
                        (String) fields.get("creator_user_id"),
                        (Map<String, Object>) fields.get("parameters")
                );
            case FIELD_VALUE:
                return new FieldValueAlertCondition(
                        core,
                        stream,
                        (String) fields.get("id"),
                        DateTime.parse((String) fields.get("created_at")),
                        (String) fields.get("creator_user_id"),
                        (Map<String, Object>) fields.get("parameters")
                );
        }

        throw new NoSuchAlertConditionTypeException("Unhandled alert condition type: " + type);
    }

    public CheckResult triggeredNoGrace() {
        LOG.debug("Checking alert condition [{}] and not accounting grace time.", this);
        return runCheck();
    }

    public CheckResult triggered() {
        LOG.debug("Checking alert condition [{}]", this);

        if(inGracePeriod()) {
            LOG.debug("Alert condition [{}] is in grace period. Not triggered.", this);
            return new CheckResult(false);
        }

        return runCheck();
    }

    public String getId() {
        return id;
    }

    public Stream getStream() {
        return stream;
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public Integer getBacklog() {
        Object rawParameter = getParameters().get("backlog");
        if (rawParameter != null && rawParameter instanceof Number)
            return (Integer)rawParameter;
        else
            return 0;
    }

    public boolean inGracePeriod() {
        int lastAlertSecondsAgo = Alert.triggeredSecondsAgo(stream.getId(), getId(), core);

        if (lastAlertSecondsAgo == -1 || grace == 0) {
            return false;
        }

        return lastAlertSecondsAgo < grace*60;
    }

    @Override
    public String toString() {
        return new StringBuilder().append(id).append(":").append(type)
                .append("={").append(getDescription()).append("}")
                .append(", stream:={").append(stream).append("}")
                .toString();
    }

    @Override
    public Map<String, Object> getPersistedFields() {
        return new HashMap<String, Object>() {{
            put("id", id);
            put("type", type.toString().toLowerCase());
            put("creator_user_id", creatorUserId);
            put("created_at", Tools.getISO8601String(createdAt));
            put("parameters", parameters);
        }};
    }

    public Map<String, Object> asMap() {
        return new HashMap<String, Object>() {{
            put("id", id);
            put("type", type.toString().toLowerCase());
            put("creator_user_id", creatorUserId);
            put("created_at", Tools.getISO8601String(createdAt));
            put("parameters", parameters);
            put("in_grace", inGracePeriod());
        }};
    }

    public int getGrace() {
        return grace;
    }

    public static class NoSuchAlertConditionTypeException extends Throwable {
        public NoSuchAlertConditionTypeException(String msg) {
            super(msg);
        }
    }

    public static class CheckResult {

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
