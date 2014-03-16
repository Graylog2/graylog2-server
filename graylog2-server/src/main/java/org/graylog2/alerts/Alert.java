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

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.Persisted;
import org.graylog2.database.validators.Validator;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Alert extends Persisted {

    private static final Logger LOG = LoggerFactory.getLogger(Alert.class);

    public static final String COLLECTION = "alerts";

    public static final int MAX_LIST_COUNT = 300;
    public static final int REST_CHECK_CACHE_SECONDS = 30;

    protected Alert(Core core, Map<String, Object> fields) {
        super(core, fields);
    }

    protected Alert(Core core, ObjectId id, Map<String, Object> fields) {
        super(core, id, fields);
    }

    public static Alert factory(AlertCondition.CheckResult checkResult, Core core) {
        Map<String, Object> fields = Maps.newHashMap();

        if (!checkResult.isTriggered()) {
            throw new RuntimeException("Tried to create alert from not triggered alert condition result.");
        }

        fields.put("triggered_at", checkResult.getTriggeredAt());
        fields.put("condition_id", checkResult.getTriggeredCondition().getId());
        fields.put("stream_id", checkResult.getTriggeredCondition().getStream().getId());
        fields.put("description", checkResult.getResultDescription());
        fields.put("condition_parameters", checkResult.getTriggeredCondition().getParameters());

        return new Alert(core, fields);
    }

    public static List<Alert> loadRecentOfStream(Core core, String streamId, DateTime since) {
        QueryBuilder qb = QueryBuilder.start("stream_id").is(streamId);

        if (since != null) {
            qb.and("triggered_at").greaterThanEquals(since.toDate());
        }

        BasicDBObject sort = new BasicDBObject("triggered_at", -1);

        final List<DBObject> alertObjects = query(
                qb.get(),
                sort,
                MAX_LIST_COUNT,
                0,
                core,
                COLLECTION
        );

        List<Alert> alerts = Lists.newArrayList();

        for (DBObject alertObj : alertObjects) {
            alerts.add(new Alert(core, (ObjectId) alertObj.get("_id"), alertObj.toMap()));
        }

        return alerts;
    }

    public static int triggeredSecondsAgo(String streamId, String conditionId, Core core) {
        DBObject query = QueryBuilder.start("stream_id").is(streamId)
                .and("condition_id").is(conditionId).get();
        BasicDBObject sort = new BasicDBObject("triggered_at", -1);

        DBObject alert = findOne(query, sort, core, COLLECTION);

        if(alert == null) {
            return -1;
        }

        DateTime triggeredAt = new DateTime(alert.get("triggered_at"));

        return Seconds.secondsBetween(triggeredAt, DateTime.now()).getSeconds();
    }

    public Map<String,Object> toMap() {
        Map<String, Object> map = Maps.newHashMap();

        DateTime triggeredAt = new DateTime(fields.get("triggered_at"));

        map.put("id", ((ObjectId) fields.get("_id")).toStringMongod());
        map.put("condition_id", fields.get("condition_id"));
        map.put("stream_id", fields.get("stream_id"));
        map.put("description", fields.get("description"));
        map.put("condition_parameters", fields.get("condition_parameters"));
        map.put("triggered_at", Tools.getISO8601String(triggeredAt));

        return map;
    }

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    @Override
    protected Map<String, Validator> getValidations() {
        return Maps.newHashMap();
    }

    @Override
    protected Map<String, Validator> getEmbeddedValidations(String key) {
        return Maps.newHashMap();
    }

}
