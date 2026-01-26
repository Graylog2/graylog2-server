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
package org.graylog2.bindings.providers;

import com.codahale.metrics.MetricRegistry;
import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.graylog.scheduler.system.SystemJobManager;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.graylog2.system.jobs.LegacySystemJobManager;

/**
 * Deprecated: Use {@link SystemJobManager} instead.
 */
@Deprecated(since = "7.1", forRemoval = true)
public class LegacySystemJobManagerProvider implements Provider<LegacySystemJobManager> {
    private static LegacySystemJobManager systemJobManager = null;

    @Inject
    public LegacySystemJobManagerProvider(ActivityWriter activityWriter, MetricRegistry metricRegistry) {
        if (systemJobManager == null)
            systemJobManager = new LegacySystemJobManager(activityWriter, metricRegistry);
    }

    @Override
    public LegacySystemJobManager get() {
        return systemJobManager;
    }
}
