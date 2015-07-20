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

import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

@CollectionName("alerts")
public class AlertImpl extends PersistedImpl implements Alert {
    private static final String FIELD_ID = "_id";
    private static final String FIELD_CONDITION_ID = "condition_id";
    private static final String FIELD_STREAM_ID = "stream_id";
    private static final String FIELD_DESCRIPTION = "description";
    private static final String FIELD_CONDITION_PARAMETERS = "condition_parameters";
    private static final String FIELD_TRIGGERED_AT = "triggered_at";

    private static final Logger LOG = LoggerFactory.getLogger(AlertImpl.class);

    public static final int MAX_LIST_COUNT = 300;
    public static final int REST_CHECK_CACHE_SECONDS = 30;

    protected AlertImpl(Map<String, Object> fields) {
        super(fields);
    }

    protected AlertImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    public Map<String,Object> toMap() {
        Map<String, Object> map = Maps.newHashMap();

        DateTime triggeredAt = new DateTime(fields.get(FIELD_TRIGGERED_AT), DateTimeZone.UTC);

        map.put("id", fields.get(FIELD_ID).toString());
        map.put(FIELD_CONDITION_ID, fields.get(FIELD_CONDITION_ID));
        map.put(FIELD_STREAM_ID, fields.get(FIELD_STREAM_ID));
        map.put(FIELD_DESCRIPTION, fields.get(FIELD_DESCRIPTION));
        map.put(FIELD_CONDITION_PARAMETERS, fields.get(FIELD_CONDITION_PARAMETERS));
        map.put(FIELD_TRIGGERED_AT, Tools.getISO8601String(triggeredAt));

        return map;
    }

    @Override
    public Map<String, Validator> getValidations() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Collections.emptyMap();
    }

    @Override
    public String getStreamId() {
        return (String) fields.get(FIELD_STREAM_ID);
    }

    @Override
    public String getConditionId() {
        return (String) fields.get(FIELD_CONDITION_ID);
    }

    @Override
    public DateTime getTriggeredAt() {
        return (DateTime) fields.get(FIELD_TRIGGERED_AT);
    }

    @Override
    public String getDescription() {
        return (String) fields.get(FIELD_DESCRIPTION);
    }

    @Override
    public Map<String, Object> getConditionParameters() {
        return (Map<String, Object>) fields.get(FIELD_CONDITION_PARAMETERS);
    }
}
