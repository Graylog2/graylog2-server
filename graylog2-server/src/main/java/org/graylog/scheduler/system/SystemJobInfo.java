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

import com.google.auto.value.AutoValue;

/**
 * Information about a system job.
 */
@AutoValue
public abstract class SystemJobInfo {
    /**
     * The type of the system job.
     *
     * @return the job type
     */
    public abstract String type();

    /**
     * A short description of the system job.
     *
     * @return the job description
     */
    public abstract String description();

    /**
     * Status information about the system job. This may include configuration details or other
     * relevant information.
     *
     * @return the job status information
     */
    public abstract String statusInfo();

    /**
     * Whether the system job can be canceled while running.
     *
     * @return true if the job is cancelable, false otherwise
     */
    public abstract boolean isCancelable();

    /**
     * Whether the system job reports progress during its execution.
     *
     * @return true if the job reports progress, false otherwise
     */
    public abstract boolean reportsProgress();

    public static Builder builder() {
        return new AutoValue_SystemJobInfo.Builder();
    }

    @AutoValue.Builder
    public static abstract class Builder {
        public abstract Builder type(String type);

        public abstract Builder description(String description);

        public abstract Builder statusInfo(String statusInfo);

        public abstract Builder isCancelable(boolean isCancelable);

        public abstract Builder reportsProgress(boolean reportsProgress);

        public abstract SystemJobInfo build();

    }
}
