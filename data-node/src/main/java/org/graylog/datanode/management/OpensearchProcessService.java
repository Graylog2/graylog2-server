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
package org.graylog.datanode.management;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog2.cluster.preflight.NodePreflightStateChangeEvent;
import org.graylog.datanode.configuration.DatanodeConfiguration;
import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class OpensearchProcessService extends AbstractIdleService implements Provider<OpensearchProcess> {

    private static final Logger LOG = LoggerFactory.getLogger(OpensearchProcessService.class);

    private static final int WATCHDOG_RESTART_ATTEMPTS = 3;
    private final OpensearchProcess process;
    private final Provider<OpensearchConfiguration> configurationProvider;

    @Inject
    public OpensearchProcessService(DatanodeConfiguration datanodeConfiguration, Provider<OpensearchConfiguration> configurationProvider, EventBus eventBus) {
        this.configurationProvider = configurationProvider;
        this.process = createOpensearchProcess(datanodeConfiguration);
        eventBus.register(this);
    }

    private OpensearchProcess createOpensearchProcess(DatanodeConfiguration datanodeConfiguration) {
        final OpensearchProcessImpl process = new OpensearchProcessImpl(datanodeConfiguration, datanodeConfiguration.processLogsBufferSize());
        final ProcessWatchdog watchdog = new ProcessWatchdog(process, WATCHDOG_RESTART_ATTEMPTS);
        process.setStateMachineTracer(watchdog);
        return process;
    }

    @Subscribe
    public void handlePreflightConfigEvent(NodePreflightStateChangeEvent event) {
        switch (event.state()) {
            case STORED -> this.process.startWithConfig(configurationProvider.get());
        }
    }

    @Override
    protected void startUp() {
        try {
            final OpensearchConfiguration config = configurationProvider.get();
            this.process.startWithConfig(config);
        } catch (OpensearchConfigurationException e) {
            LOG.warn("Failed to obtain opensearch configuration. Adapt your datanode configuration or use the preflight web interface", e);
        }
    }


    @Override
    protected void shutDown() {
        this.process.stop();
    }

    @Override
    public OpensearchProcess get() {
        return process;
    }
}
