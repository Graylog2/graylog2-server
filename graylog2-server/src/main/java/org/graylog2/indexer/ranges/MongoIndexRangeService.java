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

import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.primitives.Ints;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Comparator;
import java.util.Map;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.MoreObjects.firstNonNull;

public class MongoIndexRangeService extends PersistedServiceImpl implements IndexRangeService {
    private static final Logger LOG = LoggerFactory.getLogger(MongoIndexRangeService.class);

    private final Searches searches;
    private final ActivityWriter activityWriter;

    @Inject
    public MongoIndexRangeService(MongoConnection mongoConnection, ActivityWriter activityWriter, Searches searches) {
        super(mongoConnection);
        this.activityWriter = activityWriter;
        this.searches = searches;
    }

    @Override
    public IndexRange get(String index) throws NotFoundException {
        DBObject dbo = findOne(MongoIndexRange.class, new BasicDBObject("index", index));

        if (dbo == null)
            throw new NotFoundException("Index " + index + " not found.");

        return new MongoIndexRange((ObjectId) dbo.get("_id"), dbo.toMap());
    }

    @Override
    public SortedSet<IndexRange> getFrom(int timestamp) {
        final ImmutableSortedSet.Builder<IndexRange> ranges = ImmutableSortedSet.orderedBy(IndexRange.COMPARATOR);
        final BasicDBObject query = new BasicDBObject("start", new BasicDBObject("$gte", timestamp));
        for (DBObject dbo : query(MongoIndexRange.class, query)) {
            ranges.add(new MongoIndexRange((ObjectId) dbo.get("_id"), dbo.toMap()));
        }

        return ranges.build();
    }

    @Override
    public SortedSet<IndexRange> getFrom(DateTime dateTime) {
        return getFrom(Ints.saturatedCast(dateTime.getMillis() / 1000L));
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
        activityWriter.write(new Activity(x, MongoIndexRange.class));
    }

    @Override
    public IndexRange create(Map<String, Object> range) {
        return new MongoIndexRange(range);
    }

    @Override
    public void destroyAll() {
        destroyAll(MongoIndexRange.class);
    }

    @Override
    public IndexRange calculateRange(String index) {
        final Stopwatch x = Stopwatch.createStarted();
        final DateTime timestamp = firstNonNull(searches.findNewestMessageTimestampOfIndex(index), Tools.iso8601());
        final int rangeEnd = Ints.saturatedCast(timestamp.getMillis() / 1000L);
        final int took = Ints.saturatedCast(x.stop().elapsed(TimeUnit.MILLISECONDS));

        LOG.info("Calculated range of [{}] in [{}ms].", index, took);

        return create(ImmutableMap.<String, Object>of(
                "index", index,
                "start", rangeEnd, // FIXME The name of the attribute is massively misleading and should be rectified some time
                "calculated_at", Tools.getUTCTimestamp(),
                "took_ms", took));
    }

    @Override
    public void save(IndexRange indexRange) throws ValidationException {
        super.save(indexRange);
    }
}