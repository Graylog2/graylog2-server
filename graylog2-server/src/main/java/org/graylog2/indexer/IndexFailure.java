/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.indexer;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.Persisted;
import org.graylog2.database.validators.Validator;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class IndexFailure extends Persisted {

    public static final String COLLECTION = "index_failures";

    public IndexFailure(Map<String, Object> fields, Core core) {
        super(core, fields);
    }

    protected IndexFailure(ObjectId id, Map<String, Object> fields, Core core) {
        super(core, id, fields);
    }

    public static List<IndexFailure> all(Core core, int limit, int offset) {
        List<IndexFailure> failures = Lists.newArrayList();

        DBObject sort = new BasicDBObject();
        sort.put("$natural", -1);

        List<DBObject> results = query(new BasicDBObject(), sort, limit, offset, core, COLLECTION);
        for (DBObject o : results) {
            failures.add(new IndexFailure((ObjectId) o.get("_id"), o.toMap(), core));
        }

        return failures;
    }

    public static long countSince(Core core, DateTime since) {
        BasicDBObject query = new BasicDBObject();
        query.put("timestamp", new BasicDBObject("$gte", since.toDate()));

        return count(query, core, COLLECTION);
    }

    public Map<String, Object> asMap() {
        return new HashMap<String, Object>() {{
            put("timestamp", Tools.getISO8601String((DateTime) fields.get("timestamp")));
            put("letter_id", fields.get("letter_id"));
            put("written", fields.get("written"));
            put("message", fields.get("message"));
            put("index", fields.get("index"));
            put("type", fields.get("type"));
        }};
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
