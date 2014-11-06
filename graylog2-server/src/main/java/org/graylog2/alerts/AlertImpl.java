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

    private static final Logger LOG = LoggerFactory.getLogger(AlertImpl.class);

    public static final int MAX_LIST_COUNT = 300;
    public static final int REST_CHECK_CACHE_SECONDS = 30;

    protected AlertImpl(Map<String, Object> fields) {
        super(fields);
    }

    protected AlertImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public Map<String,Object> toMap() {
        Map<String, Object> map = Maps.newHashMap();

        DateTime triggeredAt = new DateTime(fields.get("triggered_at"), DateTimeZone.UTC);

        map.put("id", fields.get("_id").toString());
        map.put("condition_id", fields.get("condition_id"));
        map.put("stream_id", fields.get("stream_id"));
        map.put("description", fields.get("description"));
        map.put("condition_parameters", fields.get("condition_parameters"));
        map.put("triggered_at", Tools.getISO8601String(triggeredAt));

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

}
