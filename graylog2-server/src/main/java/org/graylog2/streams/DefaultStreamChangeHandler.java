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
package org.graylog2.streams;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import org.graylog2.bindings.providers.DefaultStreamProvider;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.events.StreamsChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * This class subscribes to all {@link StreamsChangedEvent} events and reloads the default stream if it has changed.
 *
 * We need this because the default Stream instance is only loaded once when it is first accessed. (see {@link DefaultStreamProvider#get()})
 * Without this, changes to the default stream would only be applied after a server restart.
 */
@Singleton
public class DefaultStreamChangeHandler {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultStreamChangeHandler.class);

    private final StreamService streamService;
    private final DefaultStreamProvider defaultStreamProvider;

    @Inject
    public DefaultStreamChangeHandler(StreamService streamService,
                                      DefaultStreamProvider defaultStreamProvider,
                                      EventBus eventBus) {
        this.streamService = streamService;
        this.defaultStreamProvider = defaultStreamProvider;

        // TODO: This class needs lifecycle management to avoid leaking objects in the EventBus
        eventBus.register(this);
    }

    @Subscribe
    public void handleStreamsChange(StreamsChangedEvent event) {
        event.streamIds().stream()
                .filter(Stream.DEFAULT_STREAM_ID::equals)
                .findFirst().ifPresent(streamId -> reloadDefaultStream());
    }

    private void reloadDefaultStream() {
        try {
            LOG.debug("Attempting to reload and set default stream");
            defaultStreamProvider.setDefaultStream(streamService.load(Stream.DEFAULT_STREAM_ID));
        } catch (Exception e) {
            LOG.error("Couldn't reload default stream", e);
        }
    }
}
