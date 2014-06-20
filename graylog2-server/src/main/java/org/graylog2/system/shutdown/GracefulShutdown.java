/*
 * Copyright 2012-2014 TORCH GmbH
 *
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.system.shutdown;

import com.google.common.util.concurrent.ServiceManager;
import com.google.inject.Inject;
import org.graylog2.Configuration;
import org.graylog2.initializers.BufferSynchronizerService;
import org.graylog2.initializers.IndexerSetupService;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.shared.ProcessingPauseLockedException;
import org.graylog2.shared.ServerStatus;
import org.graylog2.shared.initializers.InputSetupService;
import org.graylog2.shared.initializers.PeriodicalsService;
import org.graylog2.system.activities.Activity;
import org.graylog2.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Singleton
public class GracefulShutdown implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(GracefulShutdown.class);

    public final int SLEEP_SECS = 1;

    private final Configuration configuration;
    private final BufferSynchronizerService bufferSynchronizerService;
    private final IndexerSetupService indexerSetupService;
    private final PeriodicalsService periodicalsService;
    private final InputSetupService inputSetupService;
    private final ServiceManager serviceManager;
    private final ServerStatus serverStatus;
    private final ActivityWriter activityWriter;

    @Inject
    public GracefulShutdown(ServerStatus serverStatus,
                            ActivityWriter activityWriter,
                            Configuration configuration,
                            BufferSynchronizerService bufferSynchronizerService,
                            IndexerSetupService indexerSetupService,
                            PeriodicalsService periodicalsService,
                            InputSetupService inputSetupService,
                            ServiceManager serviceManager) {
        this.serverStatus = serverStatus;
        this.activityWriter = activityWriter;
        this.configuration = configuration;
        this.bufferSynchronizerService = bufferSynchronizerService;
        this.indexerSetupService = indexerSetupService;
        this.periodicalsService = periodicalsService;
        this.inputSetupService = inputSetupService;
        this.serviceManager = serviceManager;
    }

    @Override
    public void run() {
        LOG.info("Graceful shutdown initiated.");
        serverStatus.setLifecycle(Lifecycle.HALTING);

        // Give possible load balancers time to recognize state change. State is DEAD because of HALTING.
        LOG.info("Node status: [{}]. Waiting <{}sec> for possible load balancers to recognize state change.",
                serverStatus.getLifecycle().toString(),
                configuration.getLoadBalancerRecognitionPeriodSeconds());
        try {
            Thread.sleep(configuration.getLoadBalancerRecognitionPeriodSeconds()*1000);
        } catch (InterruptedException ignored) { /* nope */ }

        activityWriter.write(
                new Activity("Graceful shutdown initiated.", GracefulShutdown.class)
        );

        /*
         * Wait a second to give for example the calling REST call some time to respond
         * to the client. Using a latch or something here might be a bit over-engineered.
         */
        try {
            Thread.sleep(SLEEP_SECS*1000);
        } catch (InterruptedException ignored) { /* nope */ }

        // stop all inputs so no new messages can come in
        inputSetupService.stopAsync().awaitTerminated();

        // Make sure that message processing is enabled. We need it enabled to work on buffered/cached messages.
        serverStatus.unlockProcessingPause();
        try {
            serverStatus.resumeMessageProcessing();
            serverStatus.setLifecycle(Lifecycle.HALTING); // Was overwritten with RUNNING when resuming message processing,
        } catch (ProcessingPauseLockedException e) {
            throw new RuntimeException("Seems like unlocking the processing pause did not succeed.", e);
        }

        // flush all remaining messages from the system
        bufferSynchronizerService.stopAsync().awaitTerminated();

        // stop all maintenance tasks
        periodicalsService.stopAsync().awaitTerminated();

        // disconnect from elasticsearch
        indexerSetupService.stopAsync().awaitTerminated();

        // Waiting for the rest of the services to stop.
        try {
            serviceManager.stopAsync().awaitStopped(30, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            LOG.warn("Timeout while waiting for services to stop: {}", e);
        }

        // Shut down hard with no shutdown hooks running.
        LOG.info("Goodbye.");
        System.exit(0);
    }
}
