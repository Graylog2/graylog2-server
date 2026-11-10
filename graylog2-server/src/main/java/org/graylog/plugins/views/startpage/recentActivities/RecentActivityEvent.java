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
package org.graylog.plugins.views.startpage.recentActivities;

import org.graylog.grn.GRN;

/*
 * Used with two constructors: the 3-arg form (no itemTitle) when the title can be resolved from the catalog at read
 * time, the 4-arg form when it can not. That is the case for DELETE, because the entity is gone by then, and for
 * entity types without a content pack facade, because those never appear in the catalog at all.
 * User is not part of the catalog so we use the userName instead of the id as we don't want to look up the user for every activity
 */
public record RecentActivityEvent(ActivityType activityType, GRN grn, String itemTitle, String userName) {
    public RecentActivityEvent(ActivityType activityType, GRN grn, String userName) {
        this(activityType, grn, null, userName);
    }
}

