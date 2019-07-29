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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.events.event.Event;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.processor.EventDefinition;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

public class EventNotificationHandler {
    private static final Logger LOG = LoggerFactory.getLogger(EventNotificationHandler.class);

    private final DBJobDefinitionService jobDefinitionService;
    private final DBJobTriggerService jobTriggerService;
    private final DBNotificationService notificationService;
    private final NotificationGracePeriodService notificationGracePeriodService;

    @Inject
    public EventNotificationHandler(DBJobDefinitionService jobDefinitionService,
                                    DBJobTriggerService jobTriggerService,
                                    DBNotificationService notificationService,
                                    NotificationGracePeriodService notificationGracePeriodService) {
        this.jobDefinitionService = jobDefinitionService;
        this.jobTriggerService = jobTriggerService;
        this.notificationService = notificationService;
        this.notificationGracePeriodService = notificationGracePeriodService;
    }

    public void handleEvents(EventDefinition definition, List<EventWithContext> eventsWithContext) {
        for (Config config : definition.notifications()) {
            final Optional<JobDefinitionDto> jobDefinition =
                    jobDefinitionService.getByConfigField(Config.FIELD_NOTIFICATION_ID, config.notificationId());

            if (!jobDefinition.isPresent()) {
                LOG.error("Couldn't find job definition for notification <{}>", config.notificationId());
                continue;
            }

            final Optional<NotificationDto> notificationDto = notificationService.get(config.notificationId());
            if (!notificationDto.isPresent()) {
                LOG.error("Couldn't find notification definition for id <{}>", config.notificationId());
                continue;
            }
            final EventNotificationConfig notificationConfig = notificationDto.get().config();
            // TODO: The job trigger data needs information about the events and how to re-run the query to create the backlog

            for (EventWithContext eventWithContext : eventsWithContext) {
                final Event event = eventWithContext.event();
                if (notificationGracePeriodService.inGracePeriod(definition, config.notificationId(), event)) {
                    continue;
                }
                try {
                    final JobTriggerDto trigger = jobTriggerService.create(JobTriggerDto.builder()
                            .jobDefinitionId(jobDefinition.get().id())
                            .schedule(OnceJobSchedule.create())
                            .data(notificationConfig.toJobTriggerData(event.toDto()))
                            .build());
                    LOG.debug("Scheduled job <{}> for notification <{}> - event <{}/{}>", trigger.id(), config.notificationId(), event.getId(), event.getMessage());
                    // TODO: The trigger ID needs to be added to the "triggered_tasks" list of the event
                } catch (Exception e) {
                    LOG.error("Couldn't create job trigger for notification <{}> and event: {}", config.notificationId(), event, e);
                }
            }
        }
    }

    @AutoValue
    @JsonDeserialize(builder = Config.Builder.class)
    public static abstract class Config {
        private static final String FIELD_NOTIFICATION_ID = EventNotificationConfig.FIELD_NOTIFICATION_ID;
        private static final String FIELD_NOTIFICATION_PARAMETERS = "notification_parameters";

        @JsonProperty(FIELD_NOTIFICATION_ID)
        public abstract String notificationId();

        @JsonProperty(FIELD_NOTIFICATION_PARAMETERS)
        public abstract Optional<NotificationParameters> notificationParameters();


        public static Builder builder() {
            return Builder.create();
        }

        public abstract Builder toBuilder();

        @AutoValue.Builder
        public static abstract class Builder {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_EventNotificationHandler_Config.Builder();
            }

            @JsonProperty(FIELD_NOTIFICATION_ID)
            public abstract Builder notificationId(String notificationId);

            @JsonProperty(FIELD_NOTIFICATION_PARAMETERS)
            public abstract Builder notificationParameters(@Nullable NotificationParameters notificationParameters);

            public abstract Config build();
        }
    }
}
