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
package org.graylog.plugins.pipelineprocessor.functions.messages;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.MultimapBuilder;
import com.google.common.collect.SetMultimap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.graylog2.streams.events.StreamsChangedEvent;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@SuppressWarnings("UnstableApiUsage")
@Singleton
public class StreamCacheService extends AbstractIdleService {
    private final EventBus eventBus;
    private final StreamService streamService;
    private final ScheduledExecutorService executorService;

    private volatile SetMultimap<String, Stream> nameToStream = MultimapBuilder.hashKeys().hashSetValues().build();
    private volatile Map<String, Stream> idToStream = new HashMap<>();

    @Inject
    public StreamCacheService(EventBus eventBus,
                              StreamService streamService,
                              @Named("daemonScheduler") ScheduledExecutorService executorService) {
        this.eventBus = eventBus;
        this.streamService = streamService;
        this.executorService = executorService;
    }

    @Override
    protected void startUp() {
        updateStreams();
        eventBus.register(this);
    }

    @Override
    protected void shutDown() {
        eventBus.unregister(this);
    }

    @Subscribe
    public void handleStreamUpdate(StreamsChangedEvent event) {
        executorService.schedule(this::updateStreams, 0, TimeUnit.SECONDS);
    }

    public Collection<Stream> getByName(String name) {
        return nameToStream.get(name);
    }

    @Nullable
    public Stream getById(String id) {
        return idToStream.get(id);
    }

    @VisibleForTesting
    public void updateStreams() {
        final SetMultimap<String, Stream> streamsByName = MultimapBuilder.hashKeys().hashSetValues().build();
        final Map<String, Stream> streamById = new HashMap<>();

        streamService.loadAllEnabled().forEach(stream -> {
            streamsByName.put(stream.getTitle(), stream);
            streamById.put(stream.getId(), stream);
        });

        this.idToStream = streamById;
        this.nameToStream = streamsByName;
    }
}
