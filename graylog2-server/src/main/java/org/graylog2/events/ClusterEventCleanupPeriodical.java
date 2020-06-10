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
package org.graylog2.events;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.primitives.Ints;
import com.mongodb.WriteConcern;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.periodical.Periodical;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.concurrent.TimeUnit;

import static com.google.common.base.Preconditions.checkNotNull;

public class ClusterEventCleanupPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(ClusterEventCleanupPeriodical.class);
    private static final String COLLECTION_NAME = ClusterEventPeriodical.COLLECTION_NAME;

    @VisibleForTesting
    static final long DEFAULT_MAX_EVENT_AGE = TimeUnit.DAYS.toMillis(1L);

    private final JacksonDBCollection<ClusterEvent, String> dbCollection;
    private final long maxEventAge;

    @Inject
    public ClusterEventCleanupPeriodical(final MongoJackObjectMapperProvider mapperProvider,
                                         final MongoConnection mongoConnection) {
        this(JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                ClusterEvent.class, String.class, mapperProvider.get()), DEFAULT_MAX_EVENT_AGE);
    }

    ClusterEventCleanupPeriodical(final JacksonDBCollection<ClusterEvent, String> dbCollection, final long maxEventAge) {
        this.dbCollection = checkNotNull(dbCollection);
        this.maxEventAge = maxEventAge;
    }

    @Override
    public boolean runsForever() {
        return false;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return true;
    }

    @Override
    public boolean primaryOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return true;
    }

    @Override
    public boolean isDaemon() {
        return true;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return Ints.saturatedCast(TimeUnit.DAYS.toSeconds(1L));
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }

    @Override
    public void doRun() {
        try {
            LOG.debug("Removing stale events from MongoDB collection \"{}\"", COLLECTION_NAME);

            final long timestamp = DateTime.now(DateTimeZone.UTC).getMillis() - maxEventAge;
            final DBQuery.Query query = DBQuery.lessThan("timestamp", timestamp);
            final WriteResult<ClusterEvent, String> writeResult = dbCollection.remove(query, WriteConcern.JOURNALED);

            LOG.debug("Removed {} stale events from \"{}\"", writeResult.getN(), COLLECTION_NAME);
        } catch (Exception e) {
            LOG.warn("Error while removing stale cluster events from MongoDB", e);
        }
    }
}
