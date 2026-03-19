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

import com.fasterxml.jackson.annotation.JsonTypeName;
import com.google.auto.value.AutoValue;
import org.graylog.scheduler.JobDefinitionConfig;

/**
 * Synthetic job definition config for system jobs. We don't persist job definition configs for system jobs, that's
 * why we don't need to implement deserialization in this class.
 */
@AutoValue
@JsonTypeName(SystemJobDefinitionConfig.TYPE_NAME)
public abstract class SystemJobDefinitionConfig implements JobDefinitionConfig {
    public static final String TYPE_NAME = "system-job-v1";

    public abstract String jobFactoryType();

    public static SystemJobDefinitionConfig forJobType(String jobType) {
        return Builder.create()
                .type(SystemJobDefinitionConfig.TYPE_NAME)
                // We set the job factory type to the actual job type here because we only have a single job
                // definition config class for all system jobs.
                .jobFactoryType(jobType)
                .build();
    }

    @AutoValue.Builder
    static abstract class Builder implements JobDefinitionConfig.Builder<Builder> {
        static Builder create() {
            return new AutoValue_SystemJobDefinitionConfig.Builder();
        }

        public abstract Builder jobFactoryType(String jobFactoryType);

        public abstract SystemJobDefinitionConfig build();
    }
}
