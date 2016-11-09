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
package org.graylog2.periodical;

import com.google.common.collect.ImmutableMap;
import org.bson.types.ObjectId;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.periodical.Periodical;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.plugin.streams.StreamRuleType;
import org.graylog2.streams.StreamImpl;
import org.graylog2.streams.StreamRuleImpl;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.config.DefaultStreamCreated;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Map;

/**
 * Periodical creating the default stream if it doesn't exist.
 */
public class DefaultStreamMigrationPeriodical extends Periodical {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultStreamMigrationPeriodical.class);

    private final StreamService streamService;
    private final StreamRuleService streamRuleService;
    private final ClusterEventBus clusterEventBus;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public DefaultStreamMigrationPeriodical(final StreamService streamService,
                                            final StreamRuleService streamRuleService,
                                            final ClusterEventBus clusterEventBus,
                                            final ClusterConfigService clusterConfigService) {
        this.streamService = streamService;
        this.streamRuleService = streamRuleService;
        this.clusterEventBus = clusterEventBus;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public void doRun() {
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
        final StreamRule streamRule = new StreamRuleImpl(
                ImmutableMap.<String, Object>builder()
                        .put(StreamRuleImpl.FIELD_TYPE, StreamRuleType.ALWAYS_MATCH.getValue())
                        .put(StreamRuleImpl.FIELD_FIELD, "timestamp")
                        .put(StreamRuleImpl.FIELD_INVERTED, false)
                        .put(StreamRuleImpl.FIELD_STREAM_ID, id)
                        .put(StreamRuleImpl.FIELD_DESCRIPTION, "Match all messages")
                        .build());
        try {
            streamService.save(stream);
            streamRuleService.save(streamRule);

            LOG.info("Successfully created default stream: {}", stream.getTitle());

            clusterConfigService.write(DefaultStreamCreated.create());
            clusterEventBus.post(StreamsChangedEvent.create(stream.getId()));
        } catch (ValidationException e) {
            LOG.error("Couldn't create default stream", e);
        }
    }

    @Override
    public boolean runsForever() {
        return true;
    }

    @Override
    public boolean stopOnGracefulShutdown() {
        return false;
    }

    @Override
    public boolean masterOnly() {
        return true;
    }

    @Override
    public boolean startOnThisNode() {
        return clusterConfigService.get(DefaultStreamCreated.class) == null;
    }

    @Override
    public boolean isDaemon() {
        return false;
    }

    @Override
    public int getInitialDelaySeconds() {
        return 0;
    }

    @Override
    public int getPeriodSeconds() {
        return 0;
    }

    @Override
    protected Logger getLogger() {
        return LOG;
    }
}
