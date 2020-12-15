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
package org.graylog.scheduler.eventbus;

/**
 * A simple event that signals a scheduler job completion to subscribers.
 * We always use the same instance because there are no fields and it allows us to avoid excessive object creation.
 */
public class JobCompletedEvent {
    public static final JobCompletedEvent INSTANCE = new JobCompletedEvent();

    private JobCompletedEvent() {
    }
}
