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
import org.graylog2.shared.journal.JournalReader;
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
    private final AuditEventSender auditEventSender;
    private final JournalReader journalReader;

    @Inject
    public GracefulShutdown(ServerStatus serverStatus,
                            ActivityWriter activityWriter,
                            Configuration configuration,
                            BufferSynchronizerService bufferSynchronizerService,
                            PeriodicalsService periodicalsService,
                            InputSetupService inputSetupService,
                            JerseyService jerseyService,
                            AuditEventSender auditEventSender,
                            JournalReader journalReader) {
        this.serverStatus = serverStatus;
        this.activityWriter = activityWriter;
        this.configuration = configuration;
        this.bufferSynchronizerService = bufferSynchronizerService;
        this.periodicalsService = periodicalsService;
        this.inputSetupService = inputSetupService;
        this.jerseyService = jerseyService;
        this.auditEventSender = auditEventSender;
        this.journalReader = journalReader;
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

        journalReader.stopAsync().awaitTerminated();

        // Try to flush all remaining messages from the system
        bufferSynchronizerService.stopAsync().awaitTerminated();

        // stop all maintenance tasks
        periodicalsService.stopAsync().awaitTerminated();

        auditEventSender.success(AuditActor.system(serverStatus.getNodeId()), NODE_SHUTDOWN_COMPLETE);

        // Shut down hard with no shutdown hooks running.
        LOG.info("Goodbye.");
        if (exit) {
            System.exit(0);
        }
    }
}
