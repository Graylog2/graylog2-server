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
package org.graylog2.dashboards;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.Persisted;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.Validator;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Dashboard extends Persisted {

    private static final String COLLECTION = "dashboards";

    public Dashboard(Map<String, Object> fields, Core core) {
        super(core, fields);
    }

    protected Dashboard(ObjectId id, Map<String, Object> fields, Core core) {
        super(core, id, fields);
    }

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    public static List<Dashboard> all(Core core) {
        List<Dashboard> dashboards = Lists.newArrayList();

        List<DBObject> results = query(new BasicDBObject(), core, COLLECTION);
        for (DBObject o : results) {
            dashboards.add(new Dashboard((ObjectId) o.get("_id"), o.toMap(), core));
        }

        return dashboards;
    }

    @Override
    protected Map<String, Validator> getValidations() {
        return new HashMap<String, Validator>() {{
            put("title", new FilledStringValidator());
            put("description", new FilledStringValidator());
            put("creator_user_id", new FilledStringValidator());
            put("created_at", new DateValidator());
        }};
    }

    @Override
    protected Map<String, Validator> getEmbeddedValidations(String key) {
        return Maps.newHashMap();
    }

    public Map<String, Object> asMap() {
        // We work on the result a bit to allow correct JSON serializing.
        Map<String, Object> result = Maps.newHashMap(fields);

        result.remove("_id");
        result.put("id", ((ObjectId) fields.get("_id")).toStringMongod());

        result.remove("created_at");
        result.put("created_at", (Tools.getISO8601String((DateTime) fields.get("created_at"))));

        // TODO this sucks and should be done somewhere globally.

        return result;
    }

}
