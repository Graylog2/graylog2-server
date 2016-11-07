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

import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.CollectionName;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.streams.alerts.requests.CreateConditionRequest;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;

public class AlertServiceImpl implements AlertService {
    private final JacksonDBCollection<AlertImpl, String> coll;
    private final AlertConditionFactory alertConditionFactory;

    @Inject
    public AlertServiceImpl(MongoConnection mongoConnection,
                            MongoJackObjectMapperProvider mapperProvider,
                            AlertConditionFactory alertConditionFactory) {
        this.alertConditionFactory = alertConditionFactory;
        final String collectionName = AlertImpl.class.getAnnotation(CollectionName.class).value();
        final DBCollection dbCollection = mongoConnection.getDatabase().getCollection(collectionName);
        this.coll = JacksonDBCollection.wrap(dbCollection, AlertImpl.class, String.class, mapperProvider.get());
    }

    @Override
    public Alert factory(AlertCondition.CheckResult checkResult) {
        checkArgument(checkResult.isTriggered(), "Unable to create alert for CheckResult which is not triggered.");
        return AlertImpl.fromCheckResult(checkResult);
    }

    @Override
    public List<Alert> loadRecentOfStream(String streamId, DateTime since, int limit) {
        return Collections.unmodifiableList(this.coll.find(
            DBQuery.and(
                DBQuery.is(AlertImpl.FIELD_STREAM_ID, streamId),
                DBQuery.greaterThanEquals(AlertImpl.FIELD_TRIGGERED_AT, since)
            )
        )
            .limit(limit)
            .sort(DBSort.desc(AlertImpl.FIELD_TRIGGERED_AT))
            .toArray());
    }

    @Override
    public int triggeredSecondsAgo(String streamId, String conditionId) {
        final List<AlertImpl> mostRecentAlerts = this.coll.find(
            DBQuery.and(
                DBQuery.is(AlertImpl.FIELD_STREAM_ID, streamId),
                DBQuery.is(AlertImpl.FIELD_CONDITION_ID, conditionId)
            )
        )
            .sort(DBSort.desc(AlertImpl.FIELD_TRIGGERED_AT))
            .limit(1)
            .toArray();

        if (mostRecentAlerts == null || mostRecentAlerts.size() == 0) {
            return -1;
        }

        final Alert mostRecentAlert = mostRecentAlerts.get(0);

        return Seconds.secondsBetween(mostRecentAlert.getTriggeredAt(), Tools.nowUTC()).getSeconds();
    }

    @Override
    public long totalCount() {
        return this.coll.count();
    }

    @Override
    public long totalCountForStream(String streamId) {
        return this.coll.count(new BasicDBObject(AlertImpl.FIELD_STREAM_ID, streamId));
    }

    @Override
    public AlertCondition fromPersisted(Map<String, Object> fields, Stream stream) {
        final String type = (String)fields.get("type");

        return this.alertConditionFactory.createAlertCondition(type,
            stream,
            (String) fields.get("id"),
            DateTime.parse((String) fields.get("created_at")),
            (String) fields.get("creator_user_id"),
            (Map<String, Object>) fields.get("parameters"),
            (String) fields.get("title"));
    }

    @Override
    public AlertCondition fromRequest(CreateConditionRequest ccr, Stream stream, String userId) {
        final String type = ccr.type();
        checkArgument(type != null, "Missing alert condition type");

        return this.alertConditionFactory.createAlertCondition(type, stream, null, Tools.nowUTC(), userId, ccr.parameters(), ccr.title());
    }

    @Override
    public AlertCondition updateFromRequest(AlertCondition alertCondition, CreateConditionRequest ccr) {
        final Map<String, Object> parameters = new HashMap<>();
        parameters.putAll(alertCondition.getParameters());
        parameters.putAll(ccr.parameters());

        return this.alertConditionFactory.createAlertCondition(
            alertCondition.getType(),
            alertCondition.getStream(),
            alertCondition.getId(),
            alertCondition.getCreatedAt(),
            alertCondition.getCreatorUserId(),
            parameters,
            ccr.title()
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
    public List<Alert> listForStreamId(String streamId, int skip, int limit) {
        return Collections.unmodifiableList(this.coll.find(DBQuery.is(AlertImpl.FIELD_STREAM_ID, streamId))
            .sort(DBSort.desc(AlertImpl.FIELD_TRIGGERED_AT))
            .skip(skip)
            .limit(limit)
            .toArray());
    }

    @Override
    public Alert load(String alertId, String streamId) throws NotFoundException {
        return this.coll.findOneById(alertId);
    }

    @Override
    public String save(Alert alert) throws ValidationException {
        checkArgument(alert instanceof AlertImpl, "Supplied argument must be of type " + AlertImpl.class + ", and not " + alert.getClass());

        return this.coll.save((AlertImpl)alert).getSavedId();
    }
}
