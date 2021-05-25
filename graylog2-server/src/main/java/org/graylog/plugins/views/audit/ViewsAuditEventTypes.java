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
package org.graylog.plugins.views.audit;

import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.PluginAuditEventTypes;

import java.util.Set;

public class ViewsAuditEventTypes implements PluginAuditEventTypes {
    public static final String NAMESPACE = "views";
    private static final String PREFIX = NAMESPACE + ":";

    private static final String VIEW = "view";
    public static final String VIEW_CREATE = PREFIX + VIEW + ":create";
    public static final String VIEW_UPDATE = PREFIX + VIEW + ":update";
    public static final String VIEW_DELETE = PREFIX + VIEW + ":delete";

    private static final String VIEW_SHARING = "view_sharing";
    public static final String VIEW_SHARING_CREATE = PREFIX + VIEW_SHARING + ":create";
    public static final String VIEW_SHARING_DELETE = PREFIX + VIEW_SHARING + ":delete";

    private static final String DEFAULT_VIEW = "default_view";
    public static final String DEFAULT_VIEW_SET = PREFIX + DEFAULT_VIEW + ":set";

    private static final String SEARCH = "search";
    public static final String SEARCH_CREATE = PREFIX + SEARCH + ":create";
    public static final String SEARCH_EXECUTE = PREFIX + SEARCH + ":execute";

    private static final String SEARCH_JOB = "search_job";
    public static final String SEARCH_JOB_CREATE = PREFIX + SEARCH_JOB + ":create";

    public static final String MESSAGES = "messages";
    public static final String MESSAGES_EXPORT = PREFIX + MESSAGES + ":export";
    public static final String MESSAGES_EXPORT_REQUESTED = MESSAGES_EXPORT + "_requested";
    public static final String MESSAGES_EXPORT_SUCCEEDED = MESSAGES_EXPORT + "_succeeded";

    public static final String EXPORT_JOB = "export_job";
    public static final String EXPORT_JOB_CREATED = PREFIX + EXPORT_JOB + ":created";

    private static final ImmutableSet<String> EVENT_TYPES = ImmutableSet.<String>builder()
            .add(VIEW_CREATE)
            .add(VIEW_UPDATE)
            .add(VIEW_DELETE)

            .add(DEFAULT_VIEW_SET)

            .add(SEARCH_CREATE)
            .add(SEARCH_EXECUTE)

            .add(SEARCH_JOB_CREATE)

            .add(VIEW_SHARING_CREATE)
            .add(VIEW_SHARING_DELETE)

            .add(MESSAGES_EXPORT_REQUESTED)
            .add(MESSAGES_EXPORT_SUCCEEDED)

            .add(EXPORT_JOB_CREATED)

            .build();

    @Override
    public Set<String> auditEventTypes() {
        return EVENT_TYPES;
    }
}
