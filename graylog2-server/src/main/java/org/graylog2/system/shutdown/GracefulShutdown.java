/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package org.graylog2.system.shutdown;

import com.google.common.base.Stopwatch;
import org.graylog2.ProcessingPauseLockedException;
import org.graylog2.caches.Caches;
import org.graylog2.Core;
import org.graylog2.buffers.Buffers;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.periodical.Periodical;
import org.graylog2.plugin.inputs.InputState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.system.activities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class GracefulShutdown implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(GracefulShutdown.class);

    public final int SLEEP_SECS = 1;

    private final Core core;

    public GracefulShutdown(Core core) {
        this.core = core;
    }

    @Override
    public void run() {
        LOG.info("Graceful shutdown initiated.");
        core.setLifecycle(Lifecycle.HALTING);

        // Give possible load balancers time to recognize state change. State is DEAD because of HALTING.
        LOG.info("Node status: [{}]. Waiting <{}sec> for possible load balancers to recognize state change.",
                core.getLifecycle().toString(),
                core.getConfiguration().getLoadBalancerRecognitionPeriodSeconds());
        try {
            Thread.sleep(core.getConfiguration().getLoadBalancerRecognitionPeriodSeconds()*1000);
        } catch (InterruptedException ignored) { /* nope */ }

        core.getActivityWriter().write(
                new Activity("Graceful shutdown initiated.", GracefulShutdown.class)
        );

        /*
         * Wait a second to give for example the calling REST call some time to respond
         * to the client. Using a latch or something here might be a bit over-engineered.
         */
        try {
            Thread.sleep(SLEEP_SECS*1000);
        } catch (InterruptedException ignored) { /* nope */ }

        // Stop all inputs.
        stopInputs();

        // Make sure that message processing is enabled. We need it enabled to work on buffered/cached messages.
        core.unlockProcessingPause();
        try {
            core.resumeMessageProcessing();
            core.setLifecycle(Lifecycle.HALTING); // Was overwritten with RUNNING when resuming message processing,
        } catch (ProcessingPauseLockedException e) {
            throw new RuntimeException("Seems like unlocking the processing pause did not succeed.", e);
        }

        // Wait for empty master caches.
        Caches.waitForEmptyCaches(core);

        // Wait for buffers.
        Buffers.waitForEmptyBuffers(core);

        // Stop all threads that should be stopped.
        shutdownPeriodicals();

        // Properly close ElasticSearch node.
        core.getIndexer().getNode().close();

        // Shut down hard with no shutdown hooks running.
        LOG.info("Goodbye.");
        Runtime.getRuntime().halt(0);
    }

    private void shutdownPeriodicals() {
        for (Periodical periodical : core.periodicals().getAllStoppedOnGracefulShutdown()) {
            LOG.info("Shutting down periodical [{}].", periodical.getClass().getCanonicalName());
            Stopwatch s = new Stopwatch().start();

            // Cancel future executions.
            Map<Periodical,ScheduledFuture> futures = core.periodicals().getFutures();
            if (futures.containsKey(periodical)) {
                futures.get(periodical).cancel(false);

                s.stop();
                LOG.info("Shutdown of periodical [{}] complete, took <{}ms>.",
                        periodical.getClass().getCanonicalName(), s.elapsed(TimeUnit.MILLISECONDS));
            } else {
                LOG.error("Could not find periodical [{}] in futures list. Not stopping execution.",
                        periodical.getClass().getCanonicalName());
            }
        }
    }

    private void stopInputs() {
        for (InputState state : core.inputs().getRunningInputs()) {
            MessageInput input = state.getMessageInput();

            LOG.info("Attempting to close input <{}> [{}].", input.getUniqueReadableId(), input.getName());

            Stopwatch s = new Stopwatch().start();
            input.stop();
            s.stop();

            LOG.info("Input [{}] closed. Took [{}ms]", input.getUniqueReadableId(), s.elapsed(TimeUnit.MILLISECONDS));
        }
    }

}
