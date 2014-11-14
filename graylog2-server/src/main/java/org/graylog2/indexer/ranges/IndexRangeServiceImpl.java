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
package org.graylog2.indexer.ranges;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class IndexRangeServiceImpl extends PersistedServiceImpl implements IndexRangeService {
    private static final Logger LOG = LoggerFactory.getLogger(IndexRangeServiceImpl.class);
    private final ActivityWriter activityWriter;

    @Inject
    public IndexRangeServiceImpl(MongoConnection mongoConnection, ActivityWriter activityWriter) {
        super(mongoConnection);
        this.activityWriter = activityWriter;
    }

    @Override
    public IndexRange get(String index) throws NotFoundException {
        DBObject dbo = findOne(IndexRangeImpl.class, new BasicDBObject("index", index));

        if (dbo == null)
            throw new NotFoundException("Index " + index + " not found.");

        return new IndexRangeImpl((ObjectId) dbo.get("_id"), dbo.toMap());
    }

    @Override
    public List<IndexRange> getFrom(int timestamp) {
        List<IndexRange> ranges = Lists.newArrayList();

        BasicDBObject query = new BasicDBObject();
        query.put("start", new BasicDBObject("$gte", timestamp));

        for (DBObject dbo : query(IndexRangeImpl.class, query)) {
            ranges.add(new IndexRangeImpl((ObjectId) dbo.get("_id"), dbo.toMap()));
        }

        Collections.sort(ranges, new Comparator<IndexRange>() {
            @Override
            public int compare(IndexRange o1, IndexRange o2) {
                return o2.getStart().compareTo(o1.getStart());
            }
        });

        return ranges;
    }

    @Override
    public void destroy(String index) {
        try {
            final IndexRange range = get(index);
            destroy(range);
        } catch (NotFoundException e) {
            return;
        }

        String x = "Removed range meta-information of [" + index + "]";
        LOG.info(x);
        activityWriter.write(new Activity(x, IndexRangeImpl.class));
    }

    @Override
    public IndexRange create(Map<String, Object> range) {
        return new IndexRangeImpl(range);
    }

    @Override
    public void destroyAll() {
        destroyAll(IndexRangeImpl.class);
    }
}