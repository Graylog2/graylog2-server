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
package org.graylog2.system.shutdown;

import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog2.Configuration;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.initializers.BufferSynchronizerService;
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.initializers.InputSetupService;
import org.graylog2.shared.initializers.JerseyService;
import org.graylog2.shared.initializers.PeriodicalsService;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;

import static org.graylog2.audit.AuditEventTypes.NODE_SHUTDOWN_COMPLETE;

@Singleton
public class GracefulShutdown implements Runnable {
    private static final Logger LOG = LoggerFactory.getLogger(GracefulShutdown.class);
    private static final int SLEEP_SECS = 1;

    private final Configuration configuration;
    private final BufferSynchronizerService bufferSynchronizerService;
    private final PeriodicalsService periodicalsService;
    private final InputSetupService inputSetupService;
    private final ServerStatus serverStatus;
    private final ActivityWriter activityWriter;
    private final JerseyService jerseyService;
    private final GracefulShutdownService gracefulShutdownService;
    private final AuditEventSender auditEventSender;

    @Inject
    public GracefulShutdown(ServerStatus serverStatus,
                            ActivityWriter activityWriter,
                            Configuration configuration,
                            BufferSynchronizerService bufferSynchronizerService,
                            PeriodicalsService periodicalsService,
                            InputSetupService inputSetupService,
                            JerseyService jerseyService,
                            GracefulShutdownService gracefulShutdownService,
                            AuditEventSender auditEventSender) {
        this.serverStatus = serverStatus;
        this.activityWriter = activityWriter;
        this.configuration = configuration;
        this.bufferSynchronizerService = bufferSynchronizerService;
        this.periodicalsService = periodicalsService;
        this.inputSetupService = inputSetupService;
        this.jerseyService = jerseyService;
        this.gracefulShutdownService = gracefulShutdownService;
        this.auditEventSender = auditEventSender;
    }

    @Override
    public void run() {
        doRun(true);
    }

    public void runWithoutExit() {
        doRun(false);
    }

    private void doRun(boolean exit) {
        LOG.info("Graceful shutdown initiated.");

        // Trigger a lifecycle change. Some services are listening for those and will halt operation accordingly.
        serverStatus.shutdown();

        // Give possible load balancers time to recognize state change. State is DEAD because of HALTING.
        LOG.info("Node status: [{}]. Waiting <{}sec> for possible load balancers to recognize state change.",
                serverStatus.getLifecycle(),
                configuration.getLoadBalancerRecognitionPeriodSeconds());
        Uninterruptibles.sleepUninterruptibly(configuration.getLoadBalancerRecognitionPeriodSeconds(), TimeUnit.SECONDS);

        activityWriter.write(new Activity("Graceful shutdown initiated.", GracefulShutdown.class));

        /*
         * Wait a second to give for example the calling REST call some time to respond
         * to the client. Using a latch or something here might be a bit over-engineered.
         */
        Uninterruptibles.sleepUninterruptibly(SLEEP_SECS, TimeUnit.SECONDS);

        // Stop REST API service to avoid changes from outside.
        jerseyService.stopAsync();

        // stop all inputs so no new messages can come in
        inputSetupService.stopAsync();

        jerseyService.awaitTerminated();
        inputSetupService.awaitTerminated();

        // Try to flush all remaining messages from the system
        bufferSynchronizerService.stopAsync().awaitTerminated();

        // Stop all services that registered with the shutdown service (e.g. plugins)
        // This must run after the BufferSynchronizerService shutdown to make sure the buffers are empty.
        gracefulShutdownService.stopAsync();

        // stop all maintenance tasks
        periodicalsService.stopAsync().awaitTerminated();

        // Wait until the shutdown service is done
        gracefulShutdownService.awaitTerminated();

        auditEventSender.success(AuditActor.system(serverStatus.getNodeId()), NODE_SHUTDOWN_COMPLETE);

        // Shut down hard with no shutdown hooks running.
        LOG.info("Goodbye.");
        if (exit) {
            System.exit(0);
        }
    }
}
