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
package org.graylog2.initializers;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog2.outputs.OutputRegistry;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.system.shutdown.GracefulShutdownHook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;

@Singleton
public class OutputSetupService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(OutputSetupService.class);

    private final OutputRegistry outputRegistry;

    @Inject
    public OutputSetupService(final OutputRegistry outputRegistry,
                              final BufferSynchronizerService bufferSynchronizerService,
                              final MetricRegistry metricRegistry) {
        this.outputRegistry = outputRegistry;

        // Shutdown after the BufferSynchronizerService has stopped to avoid shutting down outputs too early.
        bufferSynchronizerService.addListener(new Listener() {
            @Override
            public void terminated(State from) {
                OutputSetupService.this.shutDownRunningOutputs();
            }
        }, executorService(metricRegistry));
    }

    private ExecutorService executorService(MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("output-setup-service-%d").build();
        return new InstrumentedExecutorService(
                Executors.newSingleThreadExecutor(threadFactory),
                metricRegistry,
                name(this.getClass(), "executor-service"));
    }

    private void shutDownRunningOutputs() {
        for (MessageOutput output : outputRegistry.getMessageOutputs()) {

            // Do not execute the stop() method for Outputs that implement the GracefulShutdown mechanism.
            if (output instanceof GracefulShutdownHook) {
                continue;
            }

            try {
                // TODO: change to debug
                LOG.info("Stopping output {}", output.getClass().getName());
                output.stop();
            } catch (Exception e) {
                LOG.error("Error stopping output", e);
            }
        }
    }

    @Override
    protected void startUp() throws Exception {
        // Outputs are started lazily in the OutputRegistry.
    }

    @Override
    protected void shutDown() throws Exception {
        // Outputs are stopped when the BufferSynchronizerService has stopped. See constructor.
    }
}
