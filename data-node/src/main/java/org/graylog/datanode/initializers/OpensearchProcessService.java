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
package org.graylog.datanode.initializers;

import com.google.common.util.concurrent.AbstractIdleService;
import org.graylog.datanode.DataNodeRunner;
import org.graylog.datanode.management.ConfigurationProvider;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.datanode.process.OpensearchProcess;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

@Singleton
public class OpensearchProcessService extends AbstractIdleService implements Provider<OpensearchProcess> {

    final private DataNodeRunner dataNodeRunner;
    final private ConfigurationProvider configurationProvider;
    private OpensearchProcess process;

    @Inject
    public OpensearchProcessService(DataNodeRunner dataNodeRunner, ConfigurationProvider configurationProvider) {
        this.dataNodeRunner = dataNodeRunner;
        this.configurationProvider = configurationProvider;
    }

    @Override
    protected void startUp() throws Exception {
        final OpensearchConfiguration opensearchConfiguration = configurationProvider.get();
        final OpensearchProcess process = dataNodeRunner.start(opensearchConfiguration);
        this.process = process;

    }

    @Override
    protected void shutDown() throws Exception {

    }

    @Override
    public OpensearchProcess get() {
        return process;
    }
}
