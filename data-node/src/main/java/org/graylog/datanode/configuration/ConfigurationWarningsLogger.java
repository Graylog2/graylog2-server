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
package org.graylog.datanode.configuration;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.datanode.opensearch.OpensearchConfigurationChangeEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ConfigurationWarningsLogger {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationWarningsLogger.class);

    @Inject
    public ConfigurationWarningsLogger(EventBus eventBus) {
        eventBus.register(this);
    }

    @Subscribe
    public void onOpensearchConfigurationChange(OpensearchConfigurationChangeEvent e) {
        final List<String> warnings = e.config().warnings();
        if (!warnings.isEmpty()) {
            final String lines = warnings.stream().collect(Collectors.joining("\n  ! ", "  ! ", ""));
            LOG.warn("""

                    ====[ CONFIGURATION WARNINGS ]====
                    {}
                    ==================================""", lines);
        }
    }
}
