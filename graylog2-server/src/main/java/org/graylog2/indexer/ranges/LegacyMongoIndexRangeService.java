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
package org.graylog2.indexer.ranges;

import com.google.common.collect.ImmutableSortedSet;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.SortedSet;

import static com.google.common.base.MoreObjects.firstNonNull;

public class LegacyMongoIndexRangeService extends PersistedServiceImpl implements IndexRangeService {
    private static final Logger LOG = LoggerFactory.getLogger(LegacyMongoIndexRangeService.class);
    private static final String COLLECTION_NAME = "index_ranges";
    private static final String FIELD_START = "start";
    private static final String FIELD_INDEX = "index";

    @Inject
    public LegacyMongoIndexRangeService(final MongoConnection mongoConnection) {
        super(mongoConnection);
    }

    @Override
    public IndexRange get(String index) throws NotFoundException {
        final DBObject dbo = findOne(new BasicDBObject(FIELD_INDEX, index), COLLECTION_NAME);

        if (dbo == null) {
            throw new NotFoundException("Index range for index <" + index + "> not found.");
        }

        try {
            return buildIndexRange(dbo);
        } catch (Exception e) {
            throw new NotFoundException("Index range for index <" + index + "> not valid.");
        }
    }

    private IndexRange buildIndexRange(DBObject dbo) {
        final ObjectId id = (ObjectId) dbo.get("_id");
        final String indexName = (String) dbo.get(FIELD_INDEX);
        final DateTime begin = new DateTime(0L, DateTimeZone.UTC);
        final DateTime end = new DateTime((Integer) dbo.get("start") * 1000L, DateTimeZone.UTC);
        final DateTime calculatedAt = new DateTime(firstNonNull((Integer) dbo.get("calculated_at"), 0) * 1000L, DateTimeZone.UTC);
        final int calculationDuration = firstNonNull((Integer) dbo.get("took_ms"), 0);

        return MongoIndexRange.create(id, indexName, begin, end, calculatedAt, calculationDuration, null);
    }

    @Override
    public SortedSet<IndexRange> find(DateTime begin, DateTime end) {
        throw new UnsupportedOperationException();
    }

    @Override
    public SortedSet<IndexRange> findAll() {
        final BasicDBList subQueries = new BasicDBList();
        subQueries.add(new BasicDBObject(FIELD_INDEX, new BasicDBObject("$exists", true)));
        subQueries.add(new BasicDBObject(FIELD_START, new BasicDBObject("$exists", true)));
        final DBObject query = new BasicDBObject("$and", subQueries);

        final List<DBObject> result = query(query, COLLECTION_NAME);

        final ImmutableSortedSet.Builder<IndexRange> indexRanges = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR);
        for (DBObject dbo : result) {
            try {
                indexRanges.add(buildIndexRange(dbo));
            } catch (Exception e) {
                LOG.debug("Couldn't add index range to result set: " + dbo, e);
            }
        }

        return indexRanges.build();
    }

    @Override
    public WriteResult<MongoIndexRange, ObjectId> save(IndexRange indexRange) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean remove(String index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IndexRange calculateRange(String index) {
        throw new UnsupportedOperationException();
    }

    @Override
    public IndexRange createUnknownRange(String index) {
        throw new UnsupportedOperationException();
    }

    public int delete(String index) {
        return destroy(new BasicDBObject(FIELD_INDEX, index), COLLECTION_NAME);
    }
}
