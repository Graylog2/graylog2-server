/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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

    public void setDefaultStream(Stream defaultStream) {
        LOG.debug("Setting new default stream: {}", defaultStream);
        this.sharedInstance.set(defaultStream);
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
