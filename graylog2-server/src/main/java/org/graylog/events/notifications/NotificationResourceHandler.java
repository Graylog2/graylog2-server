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
package org.graylog.events.notifications;

import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobDefinitionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Optional;

public class NotificationResourceHandler {
    private static final Logger LOG = LoggerFactory.getLogger(NotificationResourceHandler.class);

    private final DBNotificationService notificationService;
    private final DBJobDefinitionService jobDefinitionService;
    private final DBJobTriggerService jobTriggerService;

    @Inject
    public NotificationResourceHandler(DBNotificationService notificationService,
                                       DBJobDefinitionService jobDefinitionService,
                                       DBJobTriggerService jobTriggerService) {
        this.notificationService = notificationService;
        this.jobDefinitionService = jobDefinitionService;
        this.jobTriggerService = jobTriggerService;
    }

    /**
     * Creates a new notification definition and a corresponding scheduler job definition.
     *
     * @param unsavedDto the notification definition to save
     * @return the created event definition
     */
    public NotificationDto create(NotificationDto unsavedDto) {
        final NotificationDto dto = notificationService.save(unsavedDto);

        LOG.debug("Created notification definition <{}/{}>", dto.id(), dto.title());

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

        LOG.debug("Deleting notification definition <{}/{}>", dto.get().id(), dto.get().title());
        return notificationService.delete(dtoId) > 0;
    }

    private EventNotificationExecutionJob.Config getSchedulerConfig(String notificationId) {
        return EventNotificationExecutionJob.Config.builder()
                .notificationId(notificationId)
                .build();
    }
}
