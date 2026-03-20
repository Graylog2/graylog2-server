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
package org.graylog2.cluster.nodes.mongodb;

import org.graylog2.system.shutdown.GracefulShutdownHook;
import org.graylog2.system.shutdown.GracefulShutdownService;

import java.util.HashSet;
import java.util.Set;

public class TestShutdownService extends GracefulShutdownService {
    Set<GracefulShutdownHook> shutdownHookSet = new HashSet<>();

    @Override
    public void register(GracefulShutdownHook shutdownHook) {
        shutdownHookSet.add(shutdownHook);
    }

    @Override
    protected void shutDown() {
        shutdownHookSet.forEach(h -> {
            try {
                h.doGracefulShutdown();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
    }
}
