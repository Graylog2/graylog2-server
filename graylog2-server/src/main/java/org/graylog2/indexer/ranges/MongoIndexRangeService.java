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

import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.github.rholder.retry.WaitStrategies;
import com.google.common.base.Stopwatch;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSortedSet;
import com.google.common.eventbus.AllowConcurrentEvents;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.primitives.Ints;
import com.google.inject.Provider;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import org.bson.types.ObjectId;
import org.elasticsearch.indices.IndexClosedException;
import org.graylog2.audit.AuditActions;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.indexer.esplugin.IndexChangeMonitor;
import org.graylog2.indexer.esplugin.IndicesClosedEvent;
import org.graylog2.indexer.esplugin.IndicesDeletedEvent;
import org.graylog2.indexer.esplugin.IndicesReopenedEvent;
import org.graylog2.indexer.indices.Indices;
import org.graylog2.indexer.searches.TimestampStats;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.SortedSet;
import java.util.concurrent.TimeUnit;

public class MongoIndexRangeService implements IndexRangeService {
    private static final Logger LOG = LoggerFactory.getLogger(MongoIndexRangeService.class);
    private static final String COLLECTION_NAME = "index_ranges";

    private final Indices indices;
    private final Provider<AuditEventSender> auditEventSenderProvider;
    private final JacksonDBCollection<MongoIndexRange, ObjectId> collection;

    @Inject
    public MongoIndexRangeService(MongoConnection mongoConnection,
                                  MongoJackObjectMapperProvider objectMapperProvider,
                                  Indices indices,
                                  Provider<AuditEventSender> auditEventSenderProvider,
                                  EventBus eventBus) {
        this.indices = indices;
        this.auditEventSenderProvider = auditEventSenderProvider;
        this.collection = JacksonDBCollection.wrap(
            mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
            MongoIndexRange.class,
            ObjectId.class,
            objectMapperProvider.get());

        // This sucks. We need to bridge Elasticsearch's and our own Guice injector.
        IndexChangeMonitor.setEventBus(eventBus);
        eventBus.register(this);

        collection.createIndex(new BasicDBObject(MongoIndexRange.FIELD_INDEX_NAME, 1));
        collection.createIndex(BasicDBObjectBuilder.start()
            .add(MongoIndexRange.FIELD_BEGIN, 1)
            .add(MongoIndexRange.FIELD_END, 1)
            .get());
    }

    @Override
    public IndexRange get(String index) throws NotFoundException {
        final DBQuery.Query query = DBQuery.and(
            DBQuery.notExists("start"),
            DBQuery.is(IndexRange.FIELD_INDEX_NAME, index));
        final MongoIndexRange indexRange = collection.findOne(query);
        if (indexRange == null) {
            throw new NotFoundException("Index range for index <" + index + "> not found.");
        }

        return indexRange;
    }

    @Override
    public SortedSet<IndexRange> find(DateTime begin, DateTime end) {
        final DBCursor<MongoIndexRange> indexRanges = collection.find(
            DBQuery.or(
                DBQuery.and(
                    DBQuery.notExists("start"),  // "start" has been used by the old index ranges in MongoDB
                    DBQuery.lessThanEquals(IndexRange.FIELD_BEGIN, end.getMillis()),
                    DBQuery.greaterThanEquals(IndexRange.FIELD_END, begin.getMillis())
                ),
                DBQuery.and(
                    DBQuery.notExists("start"),  // "start" has been used by the old index ranges in MongoDB
                    DBQuery.lessThanEquals(IndexRange.FIELD_BEGIN, 0L),
                    DBQuery.greaterThanEquals(IndexRange.FIELD_END, 0L)
                )
            )
        );

        return ImmutableSortedSet.copyOf(IndexRange.COMPARATOR, (Iterator<? extends IndexRange>) indexRanges);
    }

    @Override
    public SortedSet<IndexRange> findAll() {
        return ImmutableSortedSet.copyOf(IndexRange.COMPARATOR, (Iterator<? extends IndexRange>) collection.find(DBQuery.notExists("start")));
    }

    @Override
    public IndexRange calculateRange(String index) {
        indices.waitForRecovery(index);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        final Stopwatch sw = Stopwatch.createStarted();
        final TimestampStats stats = indices.timestampStatsOfIndex(index);
        final int duration = Ints.saturatedCast(sw.stop().elapsed(TimeUnit.MILLISECONDS));

        LOG.info("Calculated range of [{}] in [{}ms].", index, duration);
        return MongoIndexRange.create(index, stats.min(), stats.max(), now, duration);
    }

    @Override
    public IndexRange createUnknownRange(String index) {
        final DateTime begin = new DateTime(0L, DateTimeZone.UTC);
        final DateTime end = new DateTime(0L, DateTimeZone.UTC);
        final DateTime now = DateTime.now(DateTimeZone.UTC);
        return MongoIndexRange.create(index, begin, end, now, 0);
    }

    @Override
    public void save(IndexRange indexRange) {
        collection.remove(DBQuery.in(IndexRange.FIELD_INDEX_NAME, indexRange.indexName()));
        collection.save(MongoIndexRange.create(indexRange));
    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleIndexDeletion(IndicesDeletedEvent event) {
        for (String index : event.indices()) {
            LOG.debug("Index \"{}\" has been deleted. Removing index range.");
            collection.remove(DBQuery.in(IndexRange.FIELD_INDEX_NAME, index));
            auditEventSenderProvider.get().success(AuditActor.system(), AuditActions.ES_INDEX_RANGE_DELETE, ImmutableMap.of("index_name", index));
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleIndexClosing(IndicesClosedEvent event) {
        for (String index : event.indices()) {
            LOG.debug("Index \"{}\" has been closed. Removing index range.");
            collection.remove(DBQuery.in(IndexRange.FIELD_INDEX_NAME, index));
            auditEventSenderProvider.get().success(AuditActor.system(), AuditActions.ES_INDEX_RANGE_DELETE, ImmutableMap.of("index_name", index));
        }
    }

    @Subscribe
    @AllowConcurrentEvents
    public void handleIndexReopening(IndicesReopenedEvent event) {
        for (final String index : event.indices()) {
            LOG.debug("Index \"{}\" has been reopened. Calculating index range.", index);

            indices.waitForRecovery(index);

            final Retryer<IndexRange> retryer = RetryerBuilder.<IndexRange>newBuilder()
                .retryIfException(input -> !(input instanceof IndexClosedException))
                .withWaitStrategy(WaitStrategies.exponentialWait())
                .withStopStrategy(StopStrategies.stopAfterDelay(5, TimeUnit.MINUTES))
                .build();

            final IndexRange indexRange;
            try {
                indexRange = retryer.call(() -> calculateRange(index));
                auditEventSenderProvider.get().success(AuditActor.system(), AuditActions.ES_INDEX_RANGE_CREATE, ImmutableMap.of("index_name", index));
            } catch (Exception e) {
                if (e.getCause() instanceof IndexClosedException) {
                    LOG.debug("Couldn't calculate index range for closed index \"" + index + "\"", e.getCause());
                    auditEventSenderProvider.get().failure(AuditActor.system(), AuditActions.ES_INDEX_RANGE_CREATE, ImmutableMap.of("index_name", index));
                    return;
                }
                LOG.error("Couldn't calculate index range for index \"" + index + "\"", e.getCause());
                auditEventSenderProvider.get().failure(AuditActor.system(), AuditActions.ES_INDEX_RANGE_CREATE, ImmutableMap.of("index_name", index));
                throw new RuntimeException("Couldn't calculate index range for index \"" + index + "\"", e);
            }

            save(indexRange);
        }
    }
}
