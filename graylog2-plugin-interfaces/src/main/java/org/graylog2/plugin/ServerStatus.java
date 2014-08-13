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

package org.graylog2.plugin;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Inject;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Singleton;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Singleton
public class ServerStatus {
    private static final Logger LOG = LoggerFactory.getLogger(ServerStatus.class);
    private final EventBus eventBus;

    public enum Capability {
        SERVER,
        RADIO,
        MASTER,
        STATSMODE,
        LOCALMODE
    }

    private final NodeId nodeId;
    private Lifecycle lifecycle;
    private final DateTime startedAt;
    private final Set<Capability> capabilitySet;

    private final AtomicBoolean isProcessing = new AtomicBoolean(true);
    private final AtomicBoolean processingPauseLocked = new AtomicBoolean(false);
    private final CountDownLatch runningLatch = new CountDownLatch(1);

    @Inject
    public ServerStatus(BaseConfiguration configuration, Set<Capability> capabilities, EventBus eventBus) {
        this.eventBus = eventBus;
        this.nodeId = new NodeId(configuration.getNodeIdFile());
        this.startedAt = new DateTime(DateTimeZone.UTC);
        this.capabilitySet = Sets.newHashSet(capabilities); // copy, because we support adding more capabilities later

        setLifecycle(Lifecycle.UNINITIALIZED);
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    public void setLifecycle(Lifecycle lifecycle) {
        // special case the two lifecycle states that influence whether processing is enabled or not.
        switch (lifecycle) {
            case RUNNING:
                isProcessing.set(true);
                runningLatch.countDown();
                break;
            case UNINITIALIZED:
            case STARTING:
            case PAUSED:
                isProcessing.set(false);
                break;
        }
        this.lifecycle = lifecycle;
        eventBus.post(this.lifecycle);
    }

    public void awaitRunning(final Runnable runnable) {
        LOG.debug("Waiting for server to enter RUNNING state");
        Uninterruptibles.awaitUninterruptibly(runningLatch);
        LOG.debug("Server entered RUNNING state");

        try {
            LOG.debug("Executing awaitRunning callback");
            runnable.run();
        } catch (Exception e) {
            LOG.error("awaitRunning callback failed", e);
        }
    }

    public DateTime getStartedAt() {
        return startedAt;
    }

    public DateTimeZone getTimezone() {
        return DateTimeZone.getDefault();
    }

    public ServerStatus addCapability(Capability capability) {
        this.capabilitySet.add(capability);
        return this;
    }

    public ServerStatus addCapabilities(Capability... capabilities) {
        this.capabilitySet.addAll(Lists.newArrayList(capabilities));
        return this;
    }

    public boolean hasCapability(Capability capability) {
        return this.capabilitySet.contains(capability);
    }

    public boolean hasCapabilities(Capability... capabilities) {
        return this.capabilitySet.containsAll(Lists.newArrayList(capabilities));
    }

    public boolean isProcessing() {
        return isProcessing.get();
    }

    public void pauseMessageProcessing() {
        pauseMessageProcessing(true);
    }

    public void pauseMessageProcessing(boolean locked) {
        setLifecycle(Lifecycle.PAUSED);

        // Never override pause lock if already locked.
        if (!processingPauseLocked.get()) {
            processingPauseLocked.set(locked);
        }
    }

    public void resumeMessageProcessing() throws ProcessingPauseLockedException {
        if (processingPauseLocked()) {
            throw new ProcessingPauseLockedException("Processing pause is locked. Wait until the locking task has finished " +
                    "or manually unlock if you know what you are doing.");
        }

        setLifecycle(Lifecycle.RUNNING);
    }

    public boolean processingPauseLocked() {
        return processingPauseLocked.get();
    }

    public void unlockProcessingPause() {
        processingPauseLocked.set(false);
    }

    public void setStatsMode(boolean statsMode) {
        if (statsMode)
            addCapability(Capability.STATSMODE);
    }

    public void setLocalMode(boolean localMode) {
        if (localMode)
            addCapability(Capability.LOCALMODE);
    }
}
