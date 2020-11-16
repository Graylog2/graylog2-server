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
package org.graylog.scheduler;

import static java.util.Objects.requireNonNull;

/**
 * This is thrown when a {@link Job} failed to execute correctly.
 */
public class JobExecutionException extends Exception {
    private final JobTriggerDto trigger;
    private final JobTriggerUpdate update;

    public JobExecutionException(String message, JobTriggerDto trigger, JobTriggerUpdate update) {
        this(message, trigger, update, null);
    }

    public JobExecutionException(String message, JobTriggerDto trigger, JobTriggerUpdate update, Throwable cause) {
        super(message, cause);
        this.trigger = requireNonNull(trigger, "trigger cannot be null");
        this.update = requireNonNull(update, "update cannot be null");
    }

    /**
     * Returns the trigger that triggered the job execution.
     *
     * @return the related trigger
     */
    public JobTriggerDto getTrigger() {
        return trigger;
    }

    /**
     * Returns the trigger update that should be stored in the database.
     *
     * @return the trigger update or null if not set
     */
    public JobTriggerUpdate getUpdate() {
        return update;
    }
}
