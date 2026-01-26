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
package org.graylog.scheduler.system;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.scheduler.Job;

import java.util.Map;
import java.util.stream.Collectors;

@Singleton
public class SystemJobFactories {
    private final Map<String, SystemJob.Factory<? extends SystemJob<? extends SystemJobConfig>>> factories;

    @Inject
    public SystemJobFactories(Map<String, SystemJob.Factory<? extends SystemJob<? extends SystemJobConfig>>> factories) {
        this.factories = factories;
    }

    /**
     * Converts the system job factories into regular job factories for use in the {@link org.graylog.scheduler.JobExecutionEngine}.
     *
     * @return a map of job type types to job factories.
     */
    public Map<String, Job.Factory<? extends Job>> getJobFactories() {
        return factories.entrySet().stream()
                .map(entry -> {
                    final SystemJob.Factory<? extends SystemJob<? extends SystemJobConfig>> factory = entry.getValue();
                    final Job.Factory<? extends Job> jobFactory = ignored -> factory.create();

                    return Map.entry(entry.getKey(), jobFactory);
                })
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
