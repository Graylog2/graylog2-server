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

import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog.scheduler.DBCustomJobDefinitionService;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.JobTriggerStatus;
import org.graylog.scheduler.schedule.CronJobSchedule;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import static org.graylog.security.certutil.CheckForCertRenewalJob.DEFINITION_INSTANCE;
import static org.graylog.security.certutil.CheckForCertRenewalJob.RENEWAL_JOB_ID;

@Singleton
public class ConfigureCertRenewalJobOnStartupService extends AbstractIdleService {
    private final DBJobDefinitionService jobDefinitionService;
    private final DBCustomJobDefinitionService customJobDefinitionService;
    private final DBJobTriggerService jobTriggerService;

    @Inject
    public ConfigureCertRenewalJobOnStartupService(final DBJobDefinitionService jobDefinitionService,
                                                   final DBCustomJobDefinitionService customJobDefinitionService,
                                                   final DBJobTriggerService jobTriggerService) {
        this.jobDefinitionService = jobDefinitionService;
        this.customJobDefinitionService = customJobDefinitionService;
        this.jobTriggerService = jobTriggerService;
    }

    @Override
    protected void startUp() throws Exception {
        // create the trigger etc. if the job definition does not exist
        if (jobDefinitionService.get(RENEWAL_JOB_ID).isEmpty()) {
            final var jobDefinition = customJobDefinitionService.findOrCreate(DEFINITION_INSTANCE);

            final var cronJobSchedule = CronJobSchedule.builder().cronExpression("0 0,30 * * * ? *").build();

            final var trigger = JobTriggerDto.builder()
                    .jobDefinitionId(jobDefinition.id())
                    .jobDefinitionType(CheckForCertRenewalJob.TYPE_NAME)
                    .schedule(cronJobSchedule)
                    .status(JobTriggerStatus.RUNNABLE)
                    .build();

            jobTriggerService.create(trigger);
        }
    }

    @Override
    protected void shutDown() throws Exception {

    }
}
