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
package org.graylog.security.certutil;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.auto.value.AutoValue;
import org.graylog.scheduler.Job;
import org.graylog.scheduler.JobDefinitionConfig;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobExecutionContext;
import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.JobTriggerUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

public class CheckForCertRenewalJob implements Job {
    private static final Logger LOG = LoggerFactory.getLogger(CheckForCertRenewalJob.class);

    public static final String TYPE_NAME = "check-for-cert-renewal-execution-v1";
    public static final String RENEWAL_JOB_ID = "64a66741cb3275652764c937";

    public static final JobDefinitionDto DEFINITION_INSTANCE = JobDefinitionDto.builder()
            .id(RENEWAL_JOB_ID) // This is a system entity and the ID MUST NOT change!
            .title("Certificat Renewal Check")
            .description("Runs periodically to check for certificates that are about to expire and notifies/triggers renewal")
            .config(CheckForCertRenewalJob.Config.builder().build())
            .build();

    private final CertRenewalService certRenewalService;

    public interface Factory extends Job.Factory<CheckForCertRenewalJob> {
        @Override
        CheckForCertRenewalJob create(JobDefinitionDto jobDefinition);
    }

    @AutoValue
    @JsonTypeName(TYPE_NAME)
    @JsonDeserialize(builder = Config.Builder.class)
    public static abstract class Config implements JobDefinitionConfig {

        public static Builder builder() {
            return Builder.create();
        }

        public abstract Builder toBuilder();

        @AutoValue.Builder
        public static abstract class Builder implements JobDefinitionConfig.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_CheckForCertRenewalJob_Config.Builder().type(TYPE_NAME);
            }

            abstract Config autoBuild();

            public Config build() {
                // Make sure the type name is correct!
                type(TYPE_NAME);
                return autoBuild();
            }
        }
    }

    @Inject
    public CheckForCertRenewalJob(final CertRenewalService certRenewalService) {
        this.certRenewalService = certRenewalService;
    }

    @Override
    public JobTriggerUpdate execute(JobExecutionContext ctx) throws JobExecutionException {
        LOG.debug("Job execute called {}", ctx);

        certRenewalService.checkCertificatesForRenewal();

        return ctx.jobTriggerUpdates()
                .scheduleNextExecution()
                .toBuilder()
                .status(JobTriggerStatus.RUNNABLE)
                .build();
    }
}
