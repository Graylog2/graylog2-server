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

import com.beust.jcommander.internal.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.activities.Activity;
import org.graylog2.database.Persisted;
import org.graylog2.database.validators.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    protected IndexRange(ObjectId id, Core core, Map<String, Object> fields) {
        super(core, id, fields);
    }

    public static IndexRange get(String index, Core core) {
        DBObject dbo = findOne(new BasicDBObject("index", index), core, COLLECTION);

        return new IndexRange((ObjectId) dbo.get("_id"), core, dbo.toMap());
    }

    public static void destroy(Core server, String index) {
        IndexRange range = IndexRange.get(index, server);
        range.destroy();

        String x = "Removed range meta-information of [" + index + "]";
        LOG.info(x);
        server.getActivityWriter().write(new Activity(x, IndexRange.class));
    }

    @Override
    public ObjectId getId() {
        return this.id;
    }

    @Override
    protected Map<String, Validator> getValidations() {
        // We don't have any, this is used internally only.
        return Maps.newHashMap();
    }

}