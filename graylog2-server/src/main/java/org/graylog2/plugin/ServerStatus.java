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
package org.graylog2.plugin;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.inject.Provider;
import org.graylog2.audit.AuditActor;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.plugin.lifecycles.Lifecycle;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.SuppressForbidden;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.graylog2.audit.AuditEventTypes.MESSAGE_PROCESSING_LOCK;

@Singleton
public class ServerStatus {
    private static final Logger LOG = LoggerFactory.getLogger(ServerStatus.class);

    public MessageDetailRecordingStrategy getDetailedMessageRecordingStrategy() {
        return messageDetailRecordingStrategy;
    }

    public enum Capability {
        SERVER,
        MASTER,
        LOCALMODE
    }

    private final EventBus eventBus;
    private final NodeId nodeId;
    private final Provider<AuditEventSender> auditEventSenderProvider;
    private final String clusterId;
    private final DateTime startedAt;
    private final Set<Capability> capabilitySet;
    private MessageDetailRecordingStrategy messageDetailRecordingStrategy = MessageDetailRecordingStrategy.NEVER;

    private final AtomicBoolean isProcessing = new AtomicBoolean(false);
    private final AtomicBoolean processingPauseLocked = new AtomicBoolean(false);
    private final CountDownLatch runningLatch = new CountDownLatch(1);

    private volatile Lifecycle lifecycle = Lifecycle.UNINITIALIZED;

    @Inject
    public ServerStatus(BaseConfiguration configuration, Set<Capability> capabilities, EventBus eventBus, Provider<AuditEventSender> auditEventSenderProvider) {
        this.eventBus = eventBus;
        this.nodeId = new NodeId(configuration.getNodeIdFile());
        this.auditEventSenderProvider = auditEventSenderProvider;
        this.clusterId = "";
        this.startedAt = Tools.nowUTC();
        this.capabilitySet = Sets.newHashSet(capabilities); // copy, because we support adding more capabilities later
        this.messageDetailRecordingStrategy = configuration.isMessageRecordingsEnabled()
                ? MessageDetailRecordingStrategy.ALWAYS
                : MessageDetailRecordingStrategy.NEVER;
    }

    public NodeId getNodeId() {
        return nodeId;
    }

    public String getClusterId() {
        return clusterId;
    }

    public Lifecycle getLifecycle() {
        return lifecycle;
    }

    private void publishLifecycle(final Lifecycle lifecycle) {
        setLifecycle(lifecycle);
        eventBus.post(lifecycle);
    }

    private void setLifecycle(final Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public void initialize() {
        publishLifecycle(Lifecycle.STARTING);
    }

    public void start() {
        isProcessing.set(true);
        runningLatch.countDown();
        publishLifecycle(Lifecycle.RUNNING);
    }

    public void shutdown(boolean forceProcessing) {
        if (forceProcessing) {
            unlockProcessingPause();
            isProcessing.set(true);
        }

        publishLifecycle(Lifecycle.HALTING);
    }

    public void shutdown() {
        shutdown(true);
    }

    public void fail() {
        isProcessing.set(false);
        publishLifecycle(Lifecycle.FAILED);
    }

    public void throttle() {
        publishLifecycle(Lifecycle.THROTTLED);
    }

    public void running() {
        publishLifecycle(Lifecycle.RUNNING);
    }

    public void overrideLoadBalancerDead() {
        publishLifecycle(Lifecycle.OVERRIDE_LB_DEAD);
    }

    public void overrideLoadBalancerAlive() {
        publishLifecycle(Lifecycle.OVERRIDE_LB_ALIVE);
    }

    public void overrideLoadBalancerThrottled() {
        publishLifecycle(Lifecycle.OVERRIDE_LB_THROTTLED);
    }

    /**
     * Blocks until the server enters the RUNNING state and then executes the given Runnable.
     * <p>
     * <b>This method is not interruptible while waiting for the server to enter the RUNNING state.</b>
     */
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

    /**
     * Blocks until the server enters the RUNNING state.
     * @throws InterruptedException if the thread is interrupted while waiting for the server to enter the RUNNING
     * state.
     */
    public void awaitRunning() throws InterruptedException {
        runningLatch.await();
    }

    public DateTime getStartedAt() {
        return startedAt;
    }

    @SuppressForbidden("Deliberate invocation")
    public DateTimeZone getTimezone() {
        return DateTimeZone.getDefault();
    }

    public ServerStatus addCapability(Capability capability) {
        this.capabilitySet.add(capability);
        return this;
    }

    public ServerStatus addCapabilities(Capability... capabilities) {
        this.capabilitySet.addAll(Arrays.asList(capabilities));
        return this;
    }

    public boolean hasCapability(Capability capability) {
        return this.capabilitySet.contains(capability);
    }

    public boolean hasCapabilities(Capability... capabilities) {
        return this.capabilitySet.containsAll(Arrays.asList(capabilities));
    }

    public boolean isProcessing() {
        return isProcessing.get();
    }

    public void pauseMessageProcessing() {
        pauseMessageProcessing(true);
    }

    public void pauseMessageProcessing(boolean locked) {
        // Never override pause lock if already locked.
        if (processingPauseLocked.compareAndSet(false, locked) && locked) {
            auditEventSenderProvider.get().success(AuditActor.system(nodeId), MESSAGE_PROCESSING_LOCK);
        } else if (locked) {
            auditEventSenderProvider.get().failure(AuditActor.system(nodeId), MESSAGE_PROCESSING_LOCK);
        }
        isProcessing.set(false);

        publishLifecycle(Lifecycle.PAUSED);
    }

    public void resumeMessageProcessing() throws ProcessingPauseLockedException {
        if (processingPauseLocked()) {
            throw new ProcessingPauseLockedException("Processing pause is locked. Wait until the locking task has finished " +
                    "or manually unlock if you know what you are doing.");
        }

        start();
    }

    public boolean processingPauseLocked() {
        return processingPauseLocked.get();
    }

    public void unlockProcessingPause() {
        processingPauseLocked.set(false);
    }

    private ServerStatus removeCapability(Capability capability) {
        this.capabilitySet.remove(capability);
        return this;
    }

    public void setLocalMode(boolean localMode) {
        if (localMode) {
            addCapability(Capability.LOCALMODE);
        } else {
            removeCapability(Capability.LOCALMODE);
        }
    }

    public enum MessageDetailRecordingStrategy {
        NEVER,
        ALWAYS;

        public boolean shouldRecord(final Message message) {
            switch (this) {
                case NEVER:
                    return false;
                case ALWAYS:
                    return true;
                default:
                    return false;
            }
        }
    }
}
