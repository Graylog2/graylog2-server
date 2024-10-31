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
package org.graylog2.notifications;

import com.google.common.util.concurrent.AbstractExecutionThreadService;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.events.processor.EventProcessorEngine;
import org.graylog.events.processor.EventProcessorException;
import org.graylog.events.processor.systemnotification.SystemNotificationEventProcessorParameters;
import org.graylog.events.processor.systemnotification.SystemNotificationRenderService;
import org.graylog.events.processor.systemnotification.SystemNotificationRenderService.RenderResponse;
import org.slf4j.Logger;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.slf4j.LoggerFactory.getLogger;

@Singleton
public class NotificationSystemEventPublisher extends AbstractExecutionThreadService {
    private static final Logger LOG = getLogger(NotificationSystemEventPublisher.class);
    private static final int MAX_QUEUED_NOTIFICATIONS = 10_000;

    private final DBEventDefinitionService dbEventDefinitionService;
    private final SystemNotificationRenderService systemNotificationRenderService;
    private final EventProcessorEngine eventProcessorEngine;
    private final ScheduledExecutorService scheduler;
    private final Duration shutdownTimeout;

    private final BlockingQueue<Notification> queuedNotifications;
    private final AtomicReference<ScheduledFuture<?>> shutdownTask = new AtomicReference<>();

    private Thread executionThread;

    @Inject
    public NotificationSystemEventPublisher(DBEventDefinitionService dbEventDefinitionService,
                                            SystemNotificationRenderService systemNotificationRenderService,
                                            EventProcessorEngine eventProcessorEngine,
                                            @Named("daemonScheduler") ScheduledExecutorService scheduler,
                                            @Named("shutdown_timeout") int shutDownTimeoutMs) {
        this.dbEventDefinitionService = dbEventDefinitionService;
        this.systemNotificationRenderService = systemNotificationRenderService;
        this.eventProcessorEngine = eventProcessorEngine;
        this.scheduler = scheduler;
        this.queuedNotifications = new LinkedBlockingQueue<>(MAX_QUEUED_NOTIFICATIONS);

        this.shutdownTimeout = Duration.ofMillis(shutDownTimeoutMs);
    }

    /**
     * <em>Asynchronously</em> publishes a notification as a system event
     *
     * @param notification Notification to publish as a system event. <b>Be aware that this method alters the
     *                     notification in place.</b>
     * @return true if the notification was accepted for publishing, false if not
     */
    public boolean submit(Notification notification) {
        try {
            if (!queuedNotifications.offer(notification)) {
                LOG.error("Unable to submit system notification for publishing as a system event. Current number of " +
                                "queued notifications: {}. Max queue size: {}", queuedNotifications.size(),
                        MAX_QUEUED_NOTIFICATIONS);
                return false;
            }
        } catch (Exception e) {
            LOG.error("Failed submitting notification for publishing as a system event", e);
            return false;
        }

        return true;
    }

    @Override
    protected void run() throws Exception {
        while (isRunning() || !queuedNotifications.isEmpty()) {

            // The shutdown task will interrupt the thread to unblock it, if it got stuck. Because the interrupt
            // exception is handled in the event publishing code, we have to break out of the run loop manually,
            // which is what we do here.
            final var task = shutdownTask.get();
            if (task != null && task.isDone()) {
                return;
            }

            try {
                final Notification notification = queuedNotifications.poll(1, TimeUnit.SECONDS);
                if (notification != null) {
                    publish(notification);
                }
            } catch (Exception e) {
                LOG.error("", e);
            }
        }
    }

    @Override
    protected void startUp() throws Exception {
        this.executionThread = Thread.currentThread();
    }

    @Override
    protected void triggerShutdown() {
        shutdownTask.set(scheduler.schedule(() -> {
            LOG.error("Notification queue was not drained within {}. Forcefully terminating.", shutdownTimeout);
            this.executionThread.interrupt();
        }, shutdownTimeout.getSeconds(), TimeUnit.SECONDS));
    }

    @Override
    protected void shutDown() throws Exception {
        if (shutdownTask.get() != null) {
            shutdownTask.get().cancel(true);
        }
    }

    private void publish(Notification notification) {

        final EventDefinitionDto systemEventDefinition =
                dbEventDefinitionService.getSystemEventDefinitions().stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("System notification event definition not found"));

        RenderResponse renderResponse;
        try {
            renderResponse = systemNotificationRenderService.render(notification);
        } catch (Exception e) {
            LOG.warn("Cannot render notification for system event publishing.", e);
            return;
        }

        notification.addDetail("message_details", renderResponse.description);
        SystemNotificationEventProcessorParameters parameters =
                SystemNotificationEventProcessorParameters.builder()
                        .notificationType(notification.getType())
                        .notificationMessage(renderResponse.title)
                        .notificationDetails(notification.getDetails())
                        .timestamp(notification.getTimestamp())
                        .build();
        try {
            eventProcessorEngine.execute(systemEventDefinition.id(), parameters);
        } catch (EventProcessorException e) {
            LOG.error("Failed to publish system event for notification {}", notification.getType().toString(), e);
        }
    }
}
