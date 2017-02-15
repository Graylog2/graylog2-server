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
package org.graylog2.bindings.providers;

import org.graylog2.database.NotFoundException;
import org.graylog2.gelfclient.util.Uninterruptibles;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Singleton
public class DefaultStreamProvider implements Provider<Stream> {
    private static final Logger LOG = LoggerFactory.getLogger(DefaultStreamProvider.class);

    private final StreamService service;

    private AtomicReference<Stream> sharedInstance = new AtomicReference<>();

    @Inject
    private DefaultStreamProvider(StreamService service) {
        this.service = service;
    }

    @Override
    public Stream get() {
        Stream defaultStream = sharedInstance.get();
        if (defaultStream != null) {
            return defaultStream;
        }

        synchronized (this) {
            defaultStream = sharedInstance.get();
            if (defaultStream != null) {
                return defaultStream;
            }
            int i = 0;
            do {
                try {
                    LOG.debug("Loading shared default stream instance");
                    defaultStream = service.load(Stream.DEFAULT_STREAM_ID);
                } catch (NotFoundException ignored) {
                    if (i % 10 == 0) {
                        LOG.warn("Unable to load default stream, tried {} times, retrying every 500ms. Processing is blocked until this succeeds.", i + 1);
                    }
                    i++;
                    Uninterruptibles.sleepUninterruptibly(500, TimeUnit.MILLISECONDS);
                }
            } while (defaultStream == null);
            sharedInstance.set(defaultStream);
        }
        return defaultStream;
    }
}
