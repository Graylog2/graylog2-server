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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.bson.types.ObjectId;
import org.graylog2.alerts.types.FieldValueAlertCondition;
import org.graylog2.alerts.types.MessageCountAlertCondition;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.resources.streams.alerts.requests.CreateConditionRequest;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

public class AlertServiceImpl extends PersistedServiceImpl implements AlertService {
    private static final Logger LOG = LoggerFactory.getLogger(AlertServiceImpl.class);

    private final FieldValueAlertCondition.Factory fieldValueAlertFactory;
    private final MessageCountAlertCondition.Factory messageCountAlertFactory;

    @Inject
    public AlertServiceImpl(MongoConnection mongoConnection, FieldValueAlertCondition.Factory fieldValueAlertFactory, MessageCountAlertCondition.Factory messageCountAlertFactory) {
        super(mongoConnection);
        this.fieldValueAlertFactory = fieldValueAlertFactory;
        this.messageCountAlertFactory = messageCountAlertFactory;
    }

    @Override
    public Alert factory(AlertCondition.CheckResult checkResult) {
        if (!checkResult.isTriggered()) {
            throw new RuntimeException("Tried to create alert from not triggered alert condition result.");
        }

        Map<String, Object> fields = Maps.newHashMap();
        fields.put("triggered_at", checkResult.getTriggeredAt());
        fields.put("condition_id", checkResult.getTriggeredCondition().getId());
        fields.put("stream_id", checkResult.getTriggeredCondition().getStream().getId());
        fields.put("description", checkResult.getResultDescription());
        fields.put("condition_parameters", checkResult.getTriggeredCondition().getParameters());

        return new AlertImpl(fields);
    }

    @Override
    public List<Alert> loadRecentOfStream(String streamId, DateTime since) {
        QueryBuilder qb = QueryBuilder.start("stream_id").is(streamId);

        if (since != null) {
            qb.and("triggered_at").greaterThanEquals(since.toDate());
        }

        BasicDBObject sort = new BasicDBObject("triggered_at", -1);

        final List<DBObject> alertObjects = query(AlertImpl.class,
                qb.get(),
                sort,
                AlertImpl.MAX_LIST_COUNT,
                0
        );

        List<Alert> alerts = Lists.newArrayList();

        for (DBObject alertObj : alertObjects) {
            alerts.add(new AlertImpl(new ObjectId(alertObj.get("_id").toString()), alertObj.toMap()));
        }

        return alerts;
    }

    @Override
    public int triggeredSecondsAgo(String streamId, String conditionId) {
        DBObject query = QueryBuilder.start("stream_id").is(streamId)
                .and("condition_id").is(conditionId).get();
        BasicDBObject sort = new BasicDBObject("triggered_at", -1);

        DBObject alert = findOne(AlertImpl.class, query, sort);

        if (alert == null) {
            return -1;
        }

        DateTime triggeredAt = new DateTime(alert.get("triggered_at"), DateTimeZone.UTC);

        return Seconds.secondsBetween(triggeredAt, Tools.iso8601()).getSeconds();
    }

    @Override
    public long totalCount() {
        return collection(AlertImpl.class).count();
    }

    @Override
    public long totalCountForStream(String streamId) {
        DBObject qry = new BasicDBObject("stream_id", streamId);
        return collection(AlertImpl.class).count(qry);
    }

    @Override
    public AlertCondition fromPersisted(Map<String, Object> fields, Stream stream) throws AbstractAlertCondition.NoSuchAlertConditionTypeException {
        AbstractAlertCondition.Type type;
        try {
            type = AbstractAlertCondition.Type.valueOf(((String) fields.get("type")).toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AbstractAlertCondition.NoSuchAlertConditionTypeException("No such alert condition type: [" + fields.get("type") + "]");
        }

        return createAlertCondition(type,
                stream,
                (String) fields.get("id"),
                DateTime.parse((String) fields.get("created_at")),
                (String) fields.get("creator_user_id"),
                (Map<String, Object>) fields.get("parameters"));
    }

    private AbstractAlertCondition createAlertCondition(AbstractAlertCondition.Type type, Stream stream, String id, DateTime createdAt, String creatorId, Map<String, Object> parameters) throws AbstractAlertCondition.NoSuchAlertConditionTypeException {
        switch (type) {
            case MESSAGE_COUNT:
                return messageCountAlertFactory.createAlertCondition(stream, id, createdAt, creatorId, parameters);
            case FIELD_VALUE:
                return fieldValueAlertFactory.createAlertCondition(stream, id, createdAt, creatorId, parameters);
        }

        throw new AbstractAlertCondition.NoSuchAlertConditionTypeException("Unhandled alert condition type: " + type);
    }

    @Override
    public AbstractAlertCondition fromRequest(CreateConditionRequest ccr, Stream stream, String userId) throws AbstractAlertCondition.NoSuchAlertConditionTypeException {
        AbstractAlertCondition.Type type;
        try {
            type = AbstractAlertCondition.Type.valueOf(ccr.type.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new AbstractAlertCondition.NoSuchAlertConditionTypeException("No such alert condition type: [" + ccr.type + "]");
        }

        Map<String, Object> parameters = ccr.parameters;

        return createAlertCondition(type, stream, null, Tools.iso8601(), userId, parameters);
    }

    @Override
    public AbstractAlertCondition updateFromRequest(AlertCondition alertCondition, CreateConditionRequest ccr) throws AbstractAlertCondition.NoSuchAlertConditionTypeException {
        AbstractAlertCondition.Type type = ((AbstractAlertCondition) alertCondition).getType();

        Map<String, Object> parameters = ccr.parameters;
        for (Map.Entry<String, Object> stringObjectEntry : alertCondition.getParameters().entrySet()) {
            if (!parameters.containsKey(stringObjectEntry.getKey())) {
                parameters.put(stringObjectEntry.getKey(), stringObjectEntry.getValue());
            }
        }

        return createAlertCondition(type,
                alertCondition.getStream(),
                alertCondition.getId(),
                alertCondition.getCreatedAt(),
                alertCondition.getCreatorUserId(),
                parameters
        );
    }

    @Override
    public boolean inGracePeriod(AlertCondition alertCondition) {
        int lastAlertSecondsAgo = triggeredSecondsAgo(alertCondition.getStream().getId(), alertCondition.getId());

        if (lastAlertSecondsAgo == -1 || alertCondition.getGrace() == 0) {
            return false;
        }

        return lastAlertSecondsAgo < alertCondition.getGrace() * 60;
    }

    @Override
    public AlertCondition.CheckResult triggeredNoGrace(AlertCondition alertCondition) {
        LOG.debug("Checking alert condition [{}] and not accounting grace time.", this);
        return ((AbstractAlertCondition) alertCondition).runCheck();
    }

    @Override
    public AlertCondition.CheckResult triggered(AlertCondition alertCondition) {
        LOG.debug("Checking alert condition [{}]", this);

        if (inGracePeriod(alertCondition)) {
            LOG.debug("Alert condition [{}] is in grace period. Not triggered.", this);
            return new AbstractAlertCondition.CheckResult(false);
        }

        return ((AbstractAlertCondition) alertCondition).runCheck();
    }

    @Override
    public Map<String, Object> asMap(final AlertCondition alertCondition) {
        return ImmutableMap.<String, Object>builder()
                .put("id", alertCondition.getId())
                .put("type", alertCondition.getTypeString().toLowerCase())
                .put("creator_user_id", alertCondition.getCreatorUserId())
                .put("created_at", Tools.getISO8601String(alertCondition.getCreatedAt()))
                .put("parameters", alertCondition.getParameters())
                .put("in_grace", inGracePeriod(alertCondition))
                .build();
    }
}