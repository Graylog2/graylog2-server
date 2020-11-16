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

import org.graylog2.indexer.healing.FixDeflectorByDeleteJob;
import org.graylog2.indexer.healing.FixDeflectorByMoveJob;
import org.graylog2.system.jobs.SystemJobFactory;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class SystemJobFactoryProvider implements Provider<SystemJobFactory> {
    private static SystemJobFactory systemJobFactory = null;

    @Inject
    public SystemJobFactoryProvider(FixDeflectorByDeleteJob.Factory deleteJobFactory,
                                    FixDeflectorByMoveJob.Factory moveJobFactory) {
        if (systemJobFactory == null)
            systemJobFactory = new SystemJobFactory(moveJobFactory, deleteJobFactory);
    }

    @Override
    public SystemJobFactory get() {
        return systemJobFactory;
    }
}
