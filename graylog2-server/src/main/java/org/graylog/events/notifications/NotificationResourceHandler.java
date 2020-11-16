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
package org.graylog.events.notifications;

import com.google.common.collect.ImmutableList;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog2.plugin.database.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import java.util.Map;
import java.util.Optional;

public class NotificationResourceHandler {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationResourceHandler.class);

    private final DBNotificationService notificationService;
    private final DBJobDefinitionService jobDefinitionService;
    private final DBEventDefinitionService eventDefinitionService;
    private final Map<String, EventNotification.Factory> eventNotificationFactories;

    @Inject
    public NotificationResourceHandler(DBNotificationService notificationService,
                                       DBJobDefinitionService jobDefinitionService,
                                       DBEventDefinitionService eventDefinitionService,
                                       Map<String, EventNotification.Factory> eventNotificationFactories) {
        this.notificationService = notificationService;
        this.jobDefinitionService = jobDefinitionService;
        this.eventDefinitionService = eventDefinitionService;
        this.eventNotificationFactories = eventNotificationFactories;
    }

    /**
     * Creates a new notification definition and a corresponding scheduler job definition.
     *
     * @param unsavedDto the notification definition to save
     * @param user
     * @return the created event definition
     */
    public NotificationDto create(NotificationDto unsavedDto, Optional<User> user) {
        final NotificationDto dto;
        if (user.isPresent()) {
            dto = notificationService.saveWithOwnership(unsavedDto, user.get());
            LOG.debug("Created notification definition <{}/{}> with user <{}>", dto.id(), dto.title(), user.get());
        } else {
            dto = notificationService.save(unsavedDto);
            LOG.debug("Created notification definition <{}/{}> without user", dto.id(), dto.title());
        }

        try {
            final JobDefinitionDto unsavedJobDefinition = JobDefinitionDto.builder()
                    .title(dto.title())
                    .description(dto.description())
                    .config(getSchedulerConfig(dto.id()))
                    .build();

            JobDefinitionDto jobDefinition = jobDefinitionService.save(unsavedJobDefinition);

            LOG.debug("Created scheduler job definition <{}/{}> for notification <{}/{}>", jobDefinition.id(),
                    jobDefinition.title(), dto.id(), dto.title());

        } catch (Exception e) {
            LOG.error("Failed to create job definition for notification <{}/{}>",
                    dto.id(), dto.title(), e);
            throw e;
        }

        return dto;
    }

    /**
     * Updates an existing notification definition and its corresponding scheduler job definition.
     *
     * @param updatedDto the notification definition to update
     * @return the updated notification definition
     */
    public NotificationDto update(NotificationDto updatedDto) {
        // Grab the old record so we can revert to it if something goes wrong
        final Optional<NotificationDto> oldDto = notificationService.get(updatedDto.id());

        final NotificationDto dto = notificationService.save(updatedDto);

        LOG.debug("Updated notification definition <{}/{}>", dto.id(), dto.title());

        try {
            // Grab the old record so we can revert to it if something goes wrong
            final JobDefinitionDto oldJobDefinition = jobDefinitionService.getByConfigField(
                    EventNotificationConfig.FIELD_NOTIFICATION_ID, updatedDto.id())
                    .orElseThrow(() -> new IllegalStateException("Couldn't find job definition for notification definition <" + updatedDto.id() + ">"));

            // Update the existing object to make sure we keep the ID
            final JobDefinitionDto unsavedJobDefinition = oldJobDefinition.toBuilder()
                    .title(dto.title())
                    .description(dto.description())
                    .config(getSchedulerConfig(dto.id()))
                    .build();

            final JobDefinitionDto jobDefinition = jobDefinitionService.save(unsavedJobDefinition);

            LOG.debug("Updated scheduler job definition <{}/{}> for notification <{}/{}>", jobDefinition.id(),
                    jobDefinition.title(), dto.id(), dto.title());

        } catch (Exception e) {
            // Cleanup if anything goes wrong
            LOG.error("Reverting to old notification definition <{}/{}> because of an error updating the job definition",
                    dto.id(), dto.title(), e);
            oldDto.ifPresent(notificationService::save);
            throw e;
        }

        return dto;
    }

    /**
     * Deletes an existing notification definition and its corresponding scheduler job definition and trigger.
     *
     * @param dtoId the notification definition to delete
     * @return true if the notification definition got deleted, false otherwise
     */
    public boolean delete(String dtoId) {
        final Optional<NotificationDto> dto = notificationService.get(dtoId);
        if (!dto.isPresent()) {
            return false;
        }

        jobDefinitionService.getByConfigField(EventNotificationConfig.FIELD_NOTIFICATION_ID, dtoId)
                .ifPresent(jobDefinition -> {
                    LOG.debug("Deleting job definition <{}/{}> for notification <{}/{}>", jobDefinition.id(),
                            jobDefinition.title(), dto.get().id(), dto.get().title());
                    jobDefinitionService.delete(jobDefinition.id());
                });

        // Delete notification from existing events
        eventDefinitionService.getByNotificationId(dtoId)
            .forEach(eventDefinition -> {
                LOG.debug("Removing notification <{}/{}> from event definition <{}/{}>",
                    dto.get().id(), dto.get().title(),
                    eventDefinition.id(), eventDefinition.title());
                final ImmutableList<EventNotificationHandler.Config> notifications = eventDefinition.notifications().stream()
                    .filter(entry -> !entry.notificationId().equals(dtoId))
                    .collect(ImmutableList.toImmutableList());
                EventDefinitionDto updatedEventDto = eventDefinition.toBuilder()
                    .notifications(notifications)
                    .build();
                eventDefinitionService.save(updatedEventDto);

            });
        LOG.debug("Deleting notification definition <{}/{}>", dto.get().id(), dto.get().title());
        return notificationService.delete(dtoId) > 0;
    }

    /**
     * Tests a notification definition by executing it with a dummy event.
     *
     * @param notificationDto the notification definition to test
     * @param userName the name of the user that triggered the test
     * @throws NotFoundException if the notification definition or the notification factory cannot be found
     * @throws InternalServerErrorException if the notification definition failed to be executed
     */
    public void test(NotificationDto notificationDto, String userName) throws NotFoundException, InternalServerErrorException {
        final EventNotification.Factory eventNotificationFactory = eventNotificationFactories.get(notificationDto.config().type());
        if (eventNotificationFactory == null) {
            throw new NotFoundException("Couldn't find factory for notification type <" + notificationDto.config().type() + ">");
        }
        final EventNotificationContext notificationContext = NotificationTestData.getDummyContext(notificationDto, userName);
        final EventNotification eventNotification = eventNotificationFactory.create();
        try {
            eventNotification.execute(notificationContext);
        } catch (EventNotificationException e) {
            if (e.getCause() != null) {
                throw new InternalServerErrorException(e.getCause().getMessage(), e);
            } else {
                throw new InternalServerErrorException(e.getMessage(), e);
            }
        }
    }

    private EventNotificationExecutionJob.Config getSchedulerConfig(String notificationId) {
        return EventNotificationExecutionJob.Config.builder()
                .notificationId(notificationId)
                .build();
    }
}
