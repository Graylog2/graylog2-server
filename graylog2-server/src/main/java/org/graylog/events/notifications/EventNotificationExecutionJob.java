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
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import com.google.inject.assistedinject.Assisted;
import org.graylog.events.configuration.EventsConfigurationProvider;
import org.graylog.events.event.EventDto;
import org.graylog.events.processor.DBEventDefinitionService;
import org.graylog.events.processor.EventDefinitionDto;
import org.graylog.scheduler.Job;
import org.graylog.scheduler.JobDefinitionConfig;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobExecutionContext;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.JobTriggerData;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerUpdate;
import org.graylog2.database.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotBlank;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class EventNotificationExecutionJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(EventNotificationExecutionJob.class);
    public static final String TYPE_NAME = "notification-execution-v1";

    public interface Factory extends Job.Factory<EventNotificationExecutionJob> {
        @Override
        EventNotificationExecutionJob create(JobDefinitionDto jobDefinition);
    }

    private final Config jobConfig;
    private final DBNotificationService notificationService;
    private final DBEventDefinitionService eventDefinitionService;
    private final DBNotificationGracePeriodService notificationGracePeriodService;
    private final Map<String, EventNotification.Factory> eventNotificationFactories;
    private final EventsConfigurationProvider configurationProvider;

    @Inject
    public EventNotificationExecutionJob(@Assisted JobDefinitionDto jobDefinition,
                                         DBNotificationService dbNotificationService,
                                         DBEventDefinitionService eventDefinitionService,
                                         DBNotificationGracePeriodService notificationGracePeriodService,
                                         Map<String, EventNotification.Factory> eventNotificationFactories,
                                         EventsConfigurationProvider configurationProvider) {
        this.jobConfig = (Config) jobDefinition.config();
        this.notificationService = dbNotificationService;
        this.eventDefinitionService = eventDefinitionService;
        this.notificationGracePeriodService = notificationGracePeriodService;
        this.eventNotificationFactories = eventNotificationFactories;
        this.configurationProvider = configurationProvider;
    }

    @Override
    public JobTriggerUpdate execute(JobExecutionContext ctx) throws JobExecutionException {
        Optional<EventDefinitionDto> optionalEventDefinition;
        long gracePeriodInMS = 0;

        final JobTriggerDto trigger = ctx.trigger();
        final Optional<Data> optionalData = trigger.data().map(d -> (Data) d);

        if (!optionalData.isPresent()) {
            throw new JobExecutionException("Missing notification job data for notification <" + jobConfig.notificationId() + ">, unable to execute notification: " + ctx.definition().title(),
                    trigger, JobTriggerUpdate.withoutNextTime());
        }
        final Data data = optionalData.get();
        final EventDto eventDto = data.eventDto();

        final NotificationDto notification = notificationService.get(jobConfig.notificationId())
                .orElseThrow(() -> new JobExecutionException("Couldn't find notification <" + jobConfig.notificationId() + ">", trigger, JobTriggerUpdate.withError(trigger)));

        final EventNotification.Factory eventNotificationFactory = eventNotificationFactories.get(notification.config().type());
        if (eventNotificationFactory == null) {
            throw new JobExecutionException("Couldn't find factory for notification type <" + notification.config().type() + ">",
                    trigger,
                    ctx.jobTriggerUpdates().scheduleNextExecution());
        }

        try {
            optionalEventDefinition = Optional.ofNullable(getEventDefinition(eventDto));
            if (optionalEventDefinition.isPresent()) {
                gracePeriodInMS = optionalEventDefinition.get().notificationSettings().gracePeriodMs();
            }
        } catch (NotFoundException e) {
            LOG.error("Couldn't find event definition with ID <{}>.", eventDto.eventDefinitionId());
            optionalEventDefinition = Optional.empty();
        }

        EventNotificationContext notificationContext = EventNotificationContext.builder()
                .notificationId(notification.id())
                .notificationConfig(notification.config())
                .event(eventDto)
                .eventDefinition(optionalEventDefinition.get())
                .jobTrigger(trigger)
                .build();

        updateTriggerStatus(eventDto, gracePeriodInMS);
        if (inGrace(eventDto, gracePeriodInMS)) {
            LOG.debug("Notification <{}> triggered but it's in grace period.", jobConfig.notificationId());
            return ctx.jobTriggerUpdates().scheduleNextExecution();
        }

        final EventNotification eventNotification = eventNotificationFactory.create();
        try {
            eventNotification.execute(notificationContext);
        } catch (TemporaryEventNotificationException e) {
            final long retryPeriod = configurationProvider.get().eventNotificationsRetry();
            throw new JobExecutionException(
                    String.format(Locale.ROOT, "Failed to execute notification, retrying in %d minutes - <%s/%s/%s>",
                            TimeUnit.MILLISECONDS.toMinutes(retryPeriod),
                            notification.id(),
                            notification.title(),
                            notification.config().type()),
                    trigger,
                    ctx.jobTriggerUpdates().retryIn(retryPeriod, TimeUnit.MILLISECONDS), e);
        } catch (PermanentEventNotificationException e) {
            throw new JobExecutionException(
                    String.format(Locale.ROOT, "Failed permanently to execute notification, giving up - <%s/%s/%s>",
                            notification.id(),
                            notification.title(),
                            notification.config().type()),
                    trigger,
                    ctx.jobTriggerUpdates().scheduleNextExecution(),
                    e);
        } catch (EventNotificationException e) {
            throw new JobExecutionException(
                    String.format(Locale.ROOT, "Notification failed to execute - <%s/%s/%s>",
                            notification.id(),
                            notification.title(),
                            notification.config().type()),
                    trigger,
                    ctx.jobTriggerUpdates().scheduleNextExecution(),
                    e);
        }
        updateNotifiedStatus(eventDto, gracePeriodInMS);
        return ctx.jobTriggerUpdates().scheduleNextExecution();
    }

    @AutoValue
    @JsonTypeName(EventNotificationExecutionJob.TYPE_NAME)
    @JsonDeserialize(builder = Config.Builder.class)
    public static abstract class Config implements JobDefinitionConfig {
        private static final String FIELD_NOTIFICATION_ID = EventNotificationConfig.FIELD_NOTIFICATION_ID;

        @JsonProperty(FIELD_NOTIFICATION_ID)
        @NotBlank
        public abstract String notificationId();

        public static Builder builder() {
            return Builder.create();
        }

        public abstract Builder toBuilder();

        @AutoValue.Builder
        public static abstract class Builder implements JobDefinitionConfig.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_EventNotificationExecutionJob_Config.Builder().type(TYPE_NAME);
            }

            @JsonProperty(FIELD_NOTIFICATION_ID)
            public abstract Builder notificationId(String notificationId);

            abstract Config autoBuild();

            public Config build() {
                // Make sure the type name is correct!
                type(TYPE_NAME);

                return autoBuild();
            }
        }
    }

    @AutoValue
    @JsonTypeName(EventNotificationExecutionJob.TYPE_NAME)
    @JsonDeserialize(builder = Data.Builder.class)
    public static abstract class Data implements JobTriggerData {
        private static final String FIELD_EVENT_DTO = "event_dto";

        @JsonProperty(FIELD_EVENT_DTO)
        public abstract EventDto eventDto();

        public static Builder builder() {
            return Builder.create();
        }

        public abstract Builder toBuilder();

        @AutoValue.Builder
        public static abstract class Builder implements JobTriggerData.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_EventNotificationExecutionJob_Data.Builder()
                        .type(TYPE_NAME);
            }

            @JsonProperty(FIELD_EVENT_DTO)
            public abstract Builder eventDto(EventDto dto);

            abstract Data autoBuild();

            public Data build() {
                // Make sure the type name is correct!
                type(TYPE_NAME);

                return autoBuild();
            }
        }
    }

    private EventDefinitionDto getEventDefinition(EventDto eventDto) throws NotFoundException {
        return eventDefinitionService.get(eventDto.eventDefinitionId()).orElseThrow(() ->
                new NotFoundException("Could not find event definition <" + eventDto.eventDefinitionId() + ">"));
    }

    private void updateTriggerStatus(EventDto eventDto, long gracePeriodInMS) {
        if (eventDto != null) {
            notificationGracePeriodService.updateTriggerStatus(
                    jobConfig.notificationId(),
                    eventDto,
                    gracePeriodInMS);
        }
    }

    private void updateNotifiedStatus(EventDto eventDto, long gracePeriodInMS) {
        if (eventDto != null) {
            notificationGracePeriodService.updateNotifiedStatus(
                    jobConfig.notificationId(),
                    eventDto,
                    gracePeriodInMS);
        }
    }

    private boolean inGrace(EventDto eventDto, long gracePeriodInMS) {
        if (gracePeriodInMS == 0) {
            return false;
        }
        try {
            boolean inGrace = notificationGracePeriodService.inGracePeriod(eventDto, jobConfig.notificationId(), gracePeriodInMS);
            if (inGrace) {
                return true;
            }
        } catch (NotFoundException e) {
            LOG.error("Couldn't find notification with ID <{}>.", jobConfig.notificationId());
        }
        return false;
    }
}
