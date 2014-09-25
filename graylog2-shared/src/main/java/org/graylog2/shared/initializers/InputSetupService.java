/**
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
package org.graylog2.shared.initializers;

import com.google.common.base.Stopwatch;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.util.concurrent.AbstractExecutionThreadService;
import com.google.common.util.concurrent.Uninterruptibles;
import org.graylog2.plugin.inputs.InputState;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.shared.inputs.InputRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Singleton
public class InputSetupService extends AbstractExecutionThreadService {
    private static final Logger LOG = LoggerFactory.getLogger(InputSetupService.class);
    private final InputRegistry inputRegistry;
    private final EventBus eventBus;

    private final CountDownLatch startLatch = new CountDownLatch(1);
    private final CountDownLatch stopLatch = new CountDownLatch(1);
    private AtomicReference<Lifecycle> previousLifecycle = new AtomicReference<>(Lifecycle.UNINITIALIZED);

    @Inject
    public InputSetupService(InputRegistry inputRegistry, EventBus eventBus) {
        this.inputRegistry = inputRegistry;
        this.eventBus = eventBus;
    }

    @Override
    protected void startUp() throws Exception {
        eventBus.register(this);
    }

    @Subscribe
    public void lifecycleChanged(Lifecycle lifecycle) {
        LOG.debug("Lifecycle is now {}", lifecycle);
        // if we switch to RUNNING from STARTING (or unknown) the server is ready to accept connections on inputs.
        // we want to postpone opening the inputs earlier, so we don't get swamped with messages before
        // we can actually process them.
        if ((lifecycle == Lifecycle.RUNNING) && (previousLifecycle.get() == Lifecycle.STARTING || previousLifecycle.get() == Lifecycle.UNINITIALIZED)) {
            LOG.info("Triggering launching persisted inputs, node transitioned from {} to {}", previousLifecycle.get(), lifecycle);

            // Set lifecycle BEFORE counting down the latch to avoid race conditions!
            previousLifecycle.set(lifecycle);
            startLatch.countDown();
        }

        // if we failed to start up due to some other service aborting, we need to get over the barrier.
        if (lifecycle == Lifecycle.FAILED) {
            startLatch.countDown();
        }
    }

    @Override
    protected void run() throws Exception {
        // prevent launching persisted inputs too early.
        LOG.debug("Delaying lauching persisted inputs until the node is in RUNNING state.");
        Uninterruptibles.awaitUninterruptibly(startLatch);

        if (previousLifecycle.get() == Lifecycle.RUNNING) {
            LOG.debug("Launching persisted inputs now.");
            inputRegistry.launchAllPersisted();
        } else {
            LOG.error("Not starting any inputs because lifecycle is: {}", previousLifecycle.get());
        }

        // next, simply block until we are asked to shutdown, even though we are consuming a thread this way.
        Uninterruptibles.awaitUninterruptibly(stopLatch);
    }

    @Override
    protected void triggerShutdown() {
        stopLatch.countDown();
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.debug("Stopping InputSetupService");
        eventBus.unregister(this);

        for (InputState state : inputRegistry.getRunningInputs()) {
            MessageInput input = state.getMessageInput();

            LOG.info("Attempting to close input <{}> [{}].", input.getUniqueReadableId(), input.getName());

            Stopwatch s = Stopwatch.createStarted();
            try {
                input.stop();

                LOG.info("Input <{}> closed. Took [{}ms]", input.getUniqueReadableId(), s.elapsed(TimeUnit.MILLISECONDS));
            } catch (Exception e) {
                LOG.error("Unable to stop input <{}> [{}]: " + e.getMessage(), input.getUniqueReadableId(), input.getName());
            } finally {
                s.stop();
            }
        }
        LOG.debug("Stopped InputSetupService");
    }
}
