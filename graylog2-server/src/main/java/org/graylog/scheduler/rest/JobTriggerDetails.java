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
package org.graylog.scheduler.rest;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class JobTriggerDetails {
    public static JobTriggerDetails EMPTY_DETAILS = create("", "", false);

    public abstract String info();

    public abstract String description();

    public abstract boolean isCancallable();

    public static JobTriggerDetails create(String info, String description, boolean isCancallable) {
        return new AutoValue_JobTriggerDetails(info, description, isCancallable);
    }

}
