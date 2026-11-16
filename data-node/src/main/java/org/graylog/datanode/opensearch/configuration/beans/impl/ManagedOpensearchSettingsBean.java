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
package org.graylog.datanode.opensearch.configuration.beans.impl;

import jakarta.inject.Inject;
import org.graylog.datanode.opensearch.configuration.OpensearchConfigurationParams;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationBean;
import org.graylog.datanode.process.configuration.beans.DatanodeConfigurationPart;
import org.graylog2.configuration.managed.ManagedConfigurationApplier;

/**
 * Hands the opensearch settings which have been stored through the web interface over to the opensearch process.
 * These settings aren't declared in a configuration bean of the data node, so they never pass through JadConfig and
 * have to be added to the opensearch configuration directly.
 */
public class ManagedOpensearchSettingsBean implements DatanodeConfigurationBean<OpensearchConfigurationParams> {
    private final ManagedConfigurationApplier managedConfigurationApplier;

    @Inject
    public ManagedOpensearchSettingsBean(ManagedConfigurationApplier managedConfigurationApplier) {
        this.managedConfigurationApplier = managedConfigurationApplier;
    }

    @Override
    public DatanodeConfigurationPart buildConfigurationPart(OpensearchConfigurationParams configurationParams) {
        return DatanodeConfigurationPart.builder()
                .properties(managedConfigurationApplier.runtimeValues())
                .build();
    }
}
