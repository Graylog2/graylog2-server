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
package org.graylog2.migrations;

import com.google.common.collect.ImmutableMap;
import org.bson.types.ObjectId;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;

/**
 * Migration creating the default stream if it doesn't exist.
 */
public class V20160929120500_CreateDefaultStreamMigration implements Migration {
    private static final Logger LOG = LoggerFactory.getLogger(V20160929120500_CreateDefaultStreamMigration.class);

    private final StreamService streamService;
    private final ClusterEventBus clusterEventBus;

    public V20160929120500_CreateDefaultStreamMigration(StreamService streamService, ClusterEventBus clusterEventBus) {
        this.streamService = streamService;
        this.clusterEventBus = clusterEventBus;
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2016-09-29T12:05:00Z");
    }

    @Override
    public void upgrade() {
        try {
            streamService.load(Stream.DEFAULT_STREAM_ID);
        } catch (NotFoundException ignored) {
            createDefaultStream();
        }
    }

    private void createDefaultStream() {
        final ObjectId id = new ObjectId(Stream.DEFAULT_STREAM_ID);
        final Map<String, Object> fields = ImmutableMap.<String, Object>builder()
                .put(StreamImpl.FIELD_TITLE, "All messages")
                .put(StreamImpl.FIELD_DESCRIPTION, "Stream containing all messages")
                .put(StreamImpl.FIELD_DISABLED, false)
                .put(StreamImpl.FIELD_CREATED_AT, DateTime.now(DateTimeZone.UTC))
                .put(StreamImpl.FIELD_CREATOR_USER_ID, "local:admin")
                .put(StreamImpl.FIELD_MATCHING_TYPE, StreamImpl.MatchingType.DEFAULT.name())
                .put(StreamImpl.FIELD_DEFAULT_STREAM, true)
                .build();
        final Stream stream = new StreamImpl(id, fields, Collections.emptyList(), Collections.emptySet(), Collections.emptySet());

        try {
            streamService.save(stream);
            LOG.info("Successfully created default stream: {}", stream.getTitle());

            clusterEventBus.post(StreamsChangedEvent.create(stream.getId()));
        } catch (ValidationException e) {
            LOG.error("Couldn't create default stream", e);
        }
    }
}
