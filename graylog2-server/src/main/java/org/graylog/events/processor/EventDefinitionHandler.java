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
package org.graylog.events.processor;

import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.clock.JobSchedulerClock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Handles event definitions and creates scheduler job definitions and job triggers.
 *
 * <b>Caveat:</b> These handlers modify different database documents without using any transactions. That means we can
 * run into concurrency issues and partial operations.
 *
 * TODO: Make use of transactions once our database library supports it
 */
public class EventDefinitionHandler {
    private static final Logger LOG = LoggerFactory.getLogger(EventDefinitionHandler.class);

    private final DBEventDefinitionService eventDefinitionService;
    private final DBJobDefinitionService jobDefinitionService;
    private final DBJobTriggerService jobTriggerService;
    private final JobSchedulerClock clock;

    @Inject
    public EventDefinitionHandler(DBEventDefinitionService eventDefinitionService,
                                  DBJobDefinitionService jobDefinitionService,
                                  DBJobTriggerService jobTriggerService,
                                  JobSchedulerClock clock) {
        this.eventDefinitionService = eventDefinitionService;
        this.jobDefinitionService = jobDefinitionService;
        this.jobTriggerService = jobTriggerService;
        this.clock = clock;
    }

    /**
     * Creates a new event definition and a corresponding scheduler job definition and trigger.
     *
     * @param unsavedDto the event definition to save
     * @return the created event definition
     */
    public EventDefinitionDto create(EventDefinitionDto unsavedDto) {
        final EventDefinitionDto dto = eventDefinitionService.save(unsavedDto);

        LOG.debug("Created event definition <{}/{}>", dto.id(), dto.title());

        try {
            dto.config().toJobSchedulerConfig(dto, clock).ifPresent(schedulerConfig -> {
                final JobDefinitionDto unsavedJobDefinition = JobDefinitionDto.builder()
                        .title(dto.title())
                        .description(dto.description())
                        .config(schedulerConfig.jobDefinitionConfig())
                        .build();

                final JobDefinitionDto jobDefinition = jobDefinitionService.save(unsavedJobDefinition);

                LOG.debug("Created scheduler job definition <{}/{}> for event definition <{}/{}>", jobDefinition.id(),
                        jobDefinition.title(), dto.id(), dto.title());

                final JobTriggerDto jobTrigger = JobTriggerDto.builderWithClock(clock)
                        .jobDefinitionId(requireNonNull(jobDefinition.id(), "Job definition ID cannot be null"))
                        .nextTime(clock.nowUTC())
                        .schedule(schedulerConfig.schedule())
                        .build();

                try {
                    final JobTriggerDto savedTrigger = jobTriggerService.create(jobTrigger);
                    LOG.debug("Created job trigger <{}> for job definition <{}/{}> and event definition <{}/{}>", savedTrigger.id(),
                            jobDefinition.id(), jobDefinition.title(), dto.id(), dto.title());
                } catch (Exception e) {
                    // Cleanup if anything goes wrong
                    LOG.error("Removing job definition <{}/{}> because of an error creating the job trigger",
                            jobDefinition.id(), jobDefinition.title(), e);
                    jobDefinitionService.delete(jobDefinition.id());
                    throw e;
                }
            });
        } catch (Exception e) {
            // Cleanup if anything goes wrong
            LOG.error("Removing event definition <{}/{}> because of an error creating the job definition",
                    dto.id(), dto.title(), e);
            eventDefinitionService.delete(dto.id());
            throw e;
        }

        return dto;
    }

    /**
     * Updates an existing event definition and its corresponding scheduler job definition and trigger.
     *
     * @param updatedDto the event definition to update
     * @return the updated event definition
     */
    public EventDefinitionDto update(EventDefinitionDto updatedDto) {
        // Grab the old record so we can revert to it if something goes wrong
        final Optional<EventDefinitionDto> oldDto = eventDefinitionService.get(updatedDto.id());

        final EventDefinitionDto dto = eventDefinitionService.save(updatedDto);

        LOG.debug("Updated event definition <{}/{}>", dto.id(), dto.title());

        try {
            dto.config().toJobSchedulerConfig(dto, clock).ifPresent(schedulerConfig -> {
                // Grab the old record so we can revert to it if something goes wrong
                final JobDefinitionDto oldJobDefinition = jobDefinitionService.getByConfigField(EventProcessorExecutionJob.Config.FIELD_EVENT_DEFINITION_ID, updatedDto.id())
                        .orElseThrow(() -> new IllegalStateException("Couldn't find job definition for event definition <" + updatedDto.id() + ">"));

                // Update the existing object to make sure we keep the ID
                final JobDefinitionDto unsavedJobDefinition = oldJobDefinition.toBuilder()
                        .title(dto.title())
                        .description(dto.description())
                        .config(schedulerConfig.jobDefinitionConfig())
                        .build();

                final JobDefinitionDto jobDefinition = jobDefinitionService.save(unsavedJobDefinition);

                LOG.debug("Updated scheduler job definition <{}/{}> for event definition <{}/{}>", jobDefinition.id(),
                        jobDefinition.title(), dto.id(), dto.title());

                final List<JobTriggerDto> jobTriggers = jobTriggerService.getForJob(jobDefinition.id());
                if (!jobTriggers.isEmpty()) {
                    // DBJobTriggerService#getForJob currently returns only one trigger. (raises an exception otherwise)
                    // Once we allow multiple triggers per job definition, this code will fail. We need some kind of label
                    // to figure out which trigger was created automatically. (e.g. event processor)
                    // TODO: Fix this code for multiple triggers per job definition
                    final JobTriggerDto jobTrigger = jobTriggers.get(0);

                    // Update the existing object to make sure we keep the ID
                    final JobTriggerDto.Builder unsavedJobTriggerBuilder = jobTrigger.toBuilder()
                            .jobDefinitionId(requireNonNull(jobDefinition.id(), "Job definition ID cannot be null"))
                            .schedule(schedulerConfig.schedule())
                            .nextTime(clock.nowUTC());

                    // Calculate new scheduling times
                    if (jobTrigger.data().isPresent()) {
                        final EventProcessorExecutionJob.Config config = (EventProcessorExecutionJob.Config) jobDefinition.config();
                        final EventProcessorExecutionJob.Data oldData = (EventProcessorExecutionJob.Data) jobTrigger.data().get();
                        EventProcessorExecutionJob.Data newData = EventProcessorExecutionJob.Data.builder()
                                .timerangeFrom(oldData.timerangeFrom())
                                .timerangeTo(oldData.timerangeFrom().plus(config.processingWindowSize()))
                                .build();
                        unsavedJobTriggerBuilder.data(newData);
                        unsavedJobTriggerBuilder.nextTime(newData.timerangeTo());
                    }

                    final JobTriggerDto unsavedJobTrigger = unsavedJobTriggerBuilder.build();
                    try {
                        jobTriggerService.update(unsavedJobTrigger);
                        LOG.debug("Updated scheduler job trigger <{}> for job definition <{}/{}> and event definition <{}/{}>",
                                unsavedJobTrigger.id(), jobDefinition.id(), jobDefinition.title(), dto.id(), dto.title());
                    } catch (Exception e) {
                        // Cleanup if anything goes wrong
                        LOG.error("Reverting to old job definition <{}/{}> because of an error updating the job trigger",
                                jobDefinition.id(), jobDefinition.title(), e);
                        jobDefinitionService.save(oldJobDefinition);
                        throw e;
                    }
                }
            });
        } catch (Exception e) {
            // Cleanup if anything goes wrong
            LOG.error("Reverting to old event definition <{}/{}> because of an error updating the job definition",
                    dto.id(), dto.title(), e);
            oldDto.ifPresent(eventDefinitionService::save);
            throw e;
        }

        return dto;
    }

    /**
     * Deletes an existing event definition and its corresponding scheduler job definition and trigger.
     *
     * @param dtoId the event definition to delete
     * @return true if the event definition got deleted, false otherwise
     */
    public boolean delete(String dtoId) {
        final Optional<EventDefinitionDto> dto = eventDefinitionService.get(dtoId);
        if (!dto.isPresent()) {
            return false;
        }

        jobDefinitionService.getByConfigField(EventProcessorExecutionJob.Config.FIELD_EVENT_DEFINITION_ID, dtoId)
                .ifPresent(jobDefinition -> {
                    final List<JobTriggerDto> jobTriggers = jobTriggerService.getForJob(jobDefinition.id());
                    // DBJobTriggerService#getForJob currently returns only one trigger. (raises an exception otherwise)
                    // Once we allow multiple triggers per job definition, this code will fail. We need some kind of label
                    // to figure out which trigger was created automatically. (e.g. event processor)
                    // TODO: Fix this code for multiple triggers per job definition
                    if (!jobTriggers.isEmpty()) {
                        LOG.debug("Deleting scheduler job trigger <{}> for job definition <{}/{}> and event definition <{}/{}>",
                                jobTriggers.get(0).id(), jobDefinition.id(), jobDefinition.title(), dto.get().id(), dto.get().title());
                        jobTriggerService.delete(jobTriggers.get(0).id());
                    }
                    LOG.debug("Deleting job definition <{}/{}> for event definition <{}/{}>", jobDefinition.id(),
                            jobDefinition.title(), dto.get().id(), dto.get().title());
                    jobDefinitionService.delete(jobDefinition.id());
                });

        LOG.debug("Deleting event definition <{}/{}>", dto.get().id(), dto.get().title());
        return eventDefinitionService.delete(dtoId) > 0;
    }
}
