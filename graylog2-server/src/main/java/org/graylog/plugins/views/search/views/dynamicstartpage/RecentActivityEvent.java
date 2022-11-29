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
package org.graylog.plugins.views.search.views.dynamicstartpage;

import org.graylog.plugins.views.search.views.ViewDTO;

public record RecentActivityEvent(ActivityType activityType, String itemId, String itemType, String itemTitle, String userName) {
    public RecentActivityEvent(ActivityType activityType, String itemId, String itemType, String itemTitle) {
        this(activityType, itemId, itemType, itemTitle, null);
    }
    public RecentActivityEvent(ActivityType activityType, ViewDTO view, String userName) {
        this(activityType, view.id(), view.type().name(), view.title(), userName);
    }
}

