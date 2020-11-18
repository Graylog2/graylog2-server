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
package org.graylog.plugins.views.search.events;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.google.auto.value.AutoValue;
import org.graylog.plugins.views.search.SearchJob;
import org.graylog2.plugin.database.users.User;
import org.joda.time.DateTime;

@AutoValue
@JsonAutoDetect
public abstract class SearchJobExecutionEvent {
    public abstract User user();
    public abstract SearchJob searchJob();
    public abstract DateTime executionStart();

    public static SearchJobExecutionEvent create(User user, SearchJob searchJob, DateTime executionStart) {
        return new AutoValue_SearchJobExecutionEvent(user, searchJob, executionStart);
    }
}
