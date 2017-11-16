/**
 * This file is part of Graylog.
 * <p>
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * <p>
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * <p>
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.system.traffic;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;
import org.mongojack.internal.update.SingleUpdateOperationValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

public class TrafficCounterService {
    private static final Logger LOG = LoggerFactory.getLogger(TrafficCounterService.class);
    private static final String BUCKET = "bucket";

    private final JacksonDBCollection<TrafficDto, ObjectId> db;

    @Inject
    public TrafficCounterService(MongoConnection mongoConnection,
                                 MongoJackObjectMapperProvider mapper) {
        db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("traffic"),
                TrafficDto.class,
                ObjectId.class,
                mapper.get());
        db.createIndex(new BasicDBObject(BUCKET, 1), new BasicDBObject("unique", true));
    }

    private static DateTime getDayBucket(DateTime observationTime) {
        return observationTime.withMinuteOfHour(0).withSecondOfMinute(0).withMillisOfSecond(0);
    }

    public void updateTraffic(DateTime observationTime, NodeId nodeId, long inLastMinute, long outLastMinute) {
        // we bucket our database by days
        final DateTime dayBucket = getDayBucket(observationTime);

        LOG.warn("Updating traffic for node {} at {}:  in {}/ out {} bytes", nodeId.toString(), dayBucket, inLastMinute, outLastMinute);
        final WriteResult<TrafficDto, ObjectId> update = db.update(DBQuery.is("bucket", dayBucket),
                // sigh DBUpdate.inc only takes integers, but we have a long.
                new DBUpdate.Builder()
                        .addOperation("$inc", "input." + nodeId.toString(),
                                new SingleUpdateOperationValue(false, false, inLastMinute))
                        .addOperation("$inc", "output." + nodeId.toString(),
                                new SingleUpdateOperationValue(false, false, outLastMinute)),
                true, false);
        if (update.getN() == 0) {
            LOG.warn("Unable to update traffic of node {}: {}", nodeId, update);
        }
    }

    public TrafficHistogram clusterTrafficOfLastDays(Duration duration) {
        final ImmutableMap.Builder<DateTime, Long> input = ImmutableMap.builder();
        final ImmutableMap.Builder<DateTime, Long> output = ImmutableMap.builder();
        final DateTime to = getDayBucket(Tools.nowUTC());
        final DateTime from = to.minus(duration);

        final DBQuery.Query query = DBQuery.and(
                DBQuery.lessThanEquals(BUCKET, to),
                DBQuery.greaterThan(BUCKET, from)
        );
        LOG.trace("Getting cluster traffic: {}", db.serializeQuery(query).toString());
        final DBCursor<TrafficDto> cursor = db.find(query);
        cursor.forEach(trafficDto -> {
            input.put(trafficDto.bucket(), trafficDto.input().values().stream().mapToLong(Long::valueOf).sum());
            output.put(trafficDto.bucket(), trafficDto.output().values().stream().mapToLong(Long::valueOf).sum());
        });
        return TrafficHistogram.create(from, to, input.build(), output.build());
    }

    @AutoValue
    @JsonAutoDetect
    public abstract static class TrafficHistogram {
        @JsonCreator
        public static TrafficHistogram create(@JsonProperty("from") DateTime from,
                                              @JsonProperty("to") DateTime to,
                                              @JsonProperty("input") Map<DateTime, Long> input,
                                              @JsonProperty("output") Map<DateTime, Long> output) {
            return new AutoValue_TrafficCounterService_TrafficHistogram(from, to, input, output);
        }

        @JsonProperty
        public abstract DateTime from();

        @JsonProperty
        public abstract DateTime to();

        @JsonProperty
        public abstract Map<DateTime, Long> input();

        @JsonProperty
        public abstract Map<DateTime, Long> output();
    }
}
