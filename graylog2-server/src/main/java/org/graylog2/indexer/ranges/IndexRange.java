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
package org.graylog2.indexer.ranges;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.Persisted;
import org.graylog2.database.validators.Validator;
import org.graylog2.system.activities.Activity;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class IndexRange extends Persisted {

    private static final Logger LOG = LoggerFactory.getLogger(IndexRange.class);

    public static final String COLLECTION = "index_ranges";

    public IndexRange(Core core, Map<String, Object> fields) {
        super(core, fields);
    }

    protected IndexRange(ObjectId id, Core core, Map<String, Object> fields) {
        super(core, id, fields);
    }

    public static IndexRange get(String index, Core core) {
        DBObject dbo = findOne(new BasicDBObject("index", index), core, COLLECTION);

        return new IndexRange((ObjectId) dbo.get("_id"), core, dbo.toMap());
    }

    public static List<IndexRange> getFrom(Core core, int timestamp) {
        List<IndexRange> ranges = Lists.newArrayList();

        BasicDBObject query = new BasicDBObject();
        query.put("start", new BasicDBObject("$gte", timestamp));

        for (DBObject dbo : query(query, core, COLLECTION)) {
            ranges.add(new IndexRange((ObjectId) dbo.get("_id"), core, dbo.toMap()));
        }

        return ranges;
    }

    public static void destroy(Core server, String index) {
        IndexRange range = IndexRange.get(index, server);
        range.destroy();

        String x = "Removed range meta-information of [" + index + "]";
        LOG.info(x);
        server.getActivityWriter().write(new Activity(x, IndexRange.class));
    }

    public String getIndexName() {
        return (String) fields.get("index");
    }

    public DateTime getCalculatedAt() {
        if (fields.containsKey("calculated_at")) {
            int ts = (Integer) fields.get("calculated_at");
            long unixMs = ts*1000L;
            return new DateTime(unixMs, DateTimeZone.UTC);
        } else {
            return null;
        }
    }

    public DateTime getStart() {
        int ts = (Integer) fields.get("start");
        long unixMs = ts*1000L;
        return new DateTime(unixMs, DateTimeZone.UTC);
    }

    public int getCalculationTookMs() {
        if (fields.containsKey("took_ms")) {
            return (Integer) fields.get("took_ms");
        } else {
            return -1;
        }
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
        return com.google.common.collect.Maps.newHashMap();
    }

}