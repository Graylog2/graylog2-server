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
package org.graylog2.periodical;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.scheduler.Job;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobExecutionContext;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.JobSchedule;
import org.graylog.scheduler.JobTriggerData;
import org.graylog.scheduler.JobTriggerUpdate;
import org.graylog.scheduler.schedule.IntervalJobSchedule;
import org.graylog.scheduler.schedule.OnceJobSchedule;
import org.graylog2.plugin.periodical.Periodical;

import javax.inject.Inject;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class LegacyPeriodicalSchedulerJob implements Job {
    public static final String TYPE_NAME = "legacy-periodical";

    public interface Factory extends Job.Factory<LegacyPeriodicalSchedulerJob> {
        @Override
        LegacyPeriodicalSchedulerJob create(JobDefinitionDto jobDefinition);
    }

    private final Set<Periodical> periodicalSet;

    public static JobSchedule buildSchedule(Periodical periodical) {
        if (periodical.runsForever()) {
            return OnceJobSchedule.create();
        }
        return IntervalJobSchedule.builder().interval(periodical.getPeriodSeconds()).unit(TimeUnit.SECONDS).build();
    }

    @Inject
    public LegacyPeriodicalSchedulerJob(Set<Periodical> periodicalSet) {
        this.periodicalSet = periodicalSet;
    }

    @Override
    public JobTriggerUpdate execute(JobExecutionContext ctx) throws JobExecutionException {
        final Data triggerData = ctx.trigger().data().map(d -> (Data) d)
                .orElseThrow(() -> new IllegalArgumentException("Missing periodical class in trigger"));
        // TODO: not very elegant
        final Periodical periodical = periodicalSet.stream()
                .filter(p -> p.getClass().getCanonicalName().equals(triggerData.periodicalClass()))
                .findFirst()
                .orElseThrow(
                        () -> new IllegalArgumentException("Cannot find periodical for class name " + triggerData.periodicalClass()));
        try {
            // TODO: some periodicals determine if they should run, by looking at the config from graylog.conf.
            //  Here we make the assumption, that all nodes are configured equally, so that if #startOnThisNode() is false
            //  for the current node, it will be false for all the nodes in the cluster.
            //  We also can't easily move this check to where we the periodical is scheduled initially, because #startOnThisNode might
            //  check for some state in the DB (like UserPermissionMigrationPeriodical.startOnThisNode) and we need a
            //  lock around this check to avoid race conditions.
            if (!periodical.startOnThisNode()) {
                return JobTriggerUpdate.withoutNextTime();
            }
            periodical.initialize();
            periodical.run();
        } catch (Exception e) {
            throw new JobExecutionException("Exception executing legacy periodical <" + triggerData.periodicalClass() + ">",
                    ctx.trigger(), JobTriggerUpdate.withError(ctx.trigger()), e);
        }

        if (periodical.runsForever()) {
            return JobTriggerUpdate.withoutNextTime();
        } else {
            return ctx.jobTriggerUpdates().scheduleNextExecution(triggerData);
        }

    }

    @JsonTypeName(TYPE_NAME)
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class JobDefinitionConfig implements org.graylog.scheduler.JobDefinitionConfig {
        @Override
        public String type() {
            return LegacyPeriodicalSchedulerJob.TYPE_NAME;
        }
    }

    @AutoValue
    @JsonTypeName(TYPE_NAME)
    @JsonDeserialize(builder = Data.Builder.class)
    public static abstract class Data implements JobTriggerData {

        @JsonProperty("periodicalClass")
        public abstract String periodicalClass();

        public static Builder builder() {
            return Builder.create();
        }

        @AutoValue.Builder
        public abstract static class Builder implements JobTriggerData.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_LegacyPeriodicalSchedulerJob_Data.Builder().type(TYPE_NAME);
            }

            @JsonProperty("periodicalClass")
            public abstract Builder periodicalClass(String periodicalClass);

            abstract Data autoBuild();

            public Data build() {
                type(TYPE_NAME);
                return autoBuild();
            }
        }
    }
}
