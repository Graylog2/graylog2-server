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
package org.graylog2.system.jobs;

import org.graylog2.indexer.healing.FixDeflectorByDeleteJob;
import org.graylog2.indexer.healing.FixDeflectorByMoveJob;

import javax.inject.Inject;
import java.util.Locale;

public class SystemJobFactory {
    private final FixDeflectorByMoveJob.Factory fixDeflectorByMoveJobFactory;
    private final FixDeflectorByDeleteJob.Factory fixDeflectorByDeleteJobFactory;

    @Inject
    public SystemJobFactory(FixDeflectorByMoveJob.Factory fixDeflectorByMoveJobFactory,
                            FixDeflectorByDeleteJob.Factory fixDeflectorByDeleteJobFactory) {
        this.fixDeflectorByMoveJobFactory = fixDeflectorByMoveJobFactory;
        this.fixDeflectorByDeleteJobFactory = fixDeflectorByDeleteJobFactory;
    }

    public SystemJob build(String jobName) throws NoSuchJobException {
        switch (SystemJob.Type.valueOf(jobName.toUpperCase(Locale.ENGLISH))) {
            case FIX_DEFLECTOR_DELETE_INDEX:
                return fixDeflectorByDeleteJobFactory.create();
            case FIX_DEFLECTOR_MOVE_INDEX:
                return fixDeflectorByMoveJobFactory.create();
        }

        throw new NoSuchJobException("Unknown system job name \"" + jobName + "\"");
    }
}
