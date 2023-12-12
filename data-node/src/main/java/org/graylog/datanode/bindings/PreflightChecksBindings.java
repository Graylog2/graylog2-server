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
package org.graylog.datanode.bindings;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.MapBinder;
import org.graylog.datanode.bootstrap.preflight.DatanodeDirectoriesLockfileCheck;
import org.graylog.datanode.bootstrap.preflight.OpenSearchPreconditionsCheck;
import org.graylog.datanode.bootstrap.preflight.OpensearchBinPreflightCheck;
import org.graylog.datanode.bootstrap.preflight.OpensearchConfigSync;
import org.graylog.datanode.bootstrap.preflight.OpensearchDataDirCompatibilityCheck;
import org.graylog2.bootstrap.preflight.PreflightCheck;

public class PreflightChecksBindings extends AbstractModule {


    @Override
    protected void configure() {
        addPreflightCheck(OpensearchConfigSync.class);
        addPreflightCheck(OpensearchBinPreflightCheck.class);
        addPreflightCheck(DatanodeDirectoriesLockfileCheck.class);
        addPreflightCheck(OpenSearchPreconditionsCheck.class);
        addPreflightCheck(OpensearchDataDirCompatibilityCheck.class);
    }


    protected void addPreflightCheck(Class<? extends PreflightCheck> preflightCheck) {
        preflightChecksBinder().addBinding(preflightCheck.getCanonicalName()).to(preflightCheck);
    }

    protected MapBinder<String, PreflightCheck> preflightChecksBinder() {
        return MapBinder.newMapBinder(binder(), String.class, PreflightCheck.class);
    }
}
