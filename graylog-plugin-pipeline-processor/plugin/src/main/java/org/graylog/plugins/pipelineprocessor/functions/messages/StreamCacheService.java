/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.functions.messages;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Maps;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.Multimaps;
import com.google.common.collect.SortedSetMultimap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;

import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

@Singleton
public class StreamCacheService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(StreamCacheService.class);

    private final EventBus eventBus;
    private final StreamService streamService;
    private final ScheduledExecutorService executorService;

    private final SortedSetMultimap<String, Stream> nameToStream = Multimaps.synchronizedSortedSetMultimap(
            MultimapBuilder.hashKeys()
                    .treeSetValues(Comparator.comparing(Stream::getId))
                    .build());
    private final Map<String, Stream> idToStream = Maps.newConcurrentMap();

    @Inject
    public StreamCacheService(EventBus eventBus,
                              StreamService streamService,
                              @Named("daemonScheduler") ScheduledExecutorService executorService) {
        this.eventBus = eventBus;
        this.streamService = streamService;
        this.executorService = executorService;
    }

    @Override
    protected void startUp() throws Exception {
        streamService.loadAllEnabled().forEach(this::updateCache);
        eventBus.register(this);
    }

    @Override
    protected void shutDown() throws Exception {
        eventBus.unregister(this);
    }

    @Subscribe
    public void handleStreamUpdate(StreamsChangedEvent event) {
        executorService.schedule(() -> updateStreams(event.streamIds()), 0, TimeUnit.SECONDS);
    }

    @VisibleForTesting
    public void updateStreams(Collection<String> ids) {
        for (String id : ids) {
            LOG.debug("Updating stream id/title cache for id {}", id);
            try {
                final Stream stream = streamService.load(id);
                if (stream.getDisabled()) {
                    purgeCache(stream.getId());
                } else {
                    updateCache(stream);
                }
            } catch (NotFoundException e) {
                // the stream was deleted, we only have to purge the existing entries
                purgeCache(id);
            }
        }
    }

    private void purgeCache(String id) {
        final Stream stream = idToStream.remove(id);
        LOG.debug("Purging stream id/title cache for id {}, stream {}", id, stream);
        if (stream != null) {
            nameToStream.remove(stream.getTitle(), stream);
        }
    }

    private void updateCache(Stream stream) {
        LOG.debug("Updating stream id/title cache for {}/'{}'", stream.getId(), stream.getTitle());
        idToStream.put(stream.getId(), stream);
        nameToStream.put(stream.getTitle(), stream);
    }


    public Collection<Stream> getByName(String name) {
        return nameToStream.get(name);
    }

    @Nullable
    public Stream getById(String id) {
        return idToStream.get(id);
    }
}
