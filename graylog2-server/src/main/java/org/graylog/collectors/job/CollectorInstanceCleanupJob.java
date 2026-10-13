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
package org.graylog.collectors.job;

import org.graylog.scheduler.JobExecutionException;
import org.graylog.scheduler.system.SystemJob;
import org.graylog.scheduler.system.SystemJobConfig;
import org.graylog.scheduler.system.SystemJobContext;
import org.graylog.scheduler.system.SystemJobResult;

public class CollectorInstanceCleanupJob implements SystemJob<CollectorInstanceCleanupJob.Config> {
    public static final String TYPE_NAME = "cleanup-collector-instances";

    @Override
    public SystemJobResult execute(Config config, SystemJobContext ctx) throws JobExecutionException {
        return null;
    }

    public abstract static class Config implements SystemJobConfig {

    }
}
