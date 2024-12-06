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
package org.graylog2.commands;

import com.google.common.collect.Lists;
import com.google.inject.Module;
import jakarta.annotation.Nonnull;
import org.graylog2.GraylogNodeConfiguration;
import org.graylog2.bindings.GraylogNodeModule;
import org.graylog2.bootstrap.CmdLineTool;
import org.graylog2.featureflag.FeatureFlags;

import java.util.ArrayList;
import java.util.List;

/**
 * Inherit from this command to create new standalone node types.
 */
public abstract class AbstractNodeCommand extends CmdLineTool<GraylogNodeConfiguration> {

    private final GraylogNodeModule nodeModule;

    public AbstractNodeCommand(final GraylogNodeConfiguration configuration) {
        this(null, configuration);
    }

    public AbstractNodeCommand(final String commandName, final GraylogNodeConfiguration configuration) {
        super(commandName, configuration);
        this.nodeModule = new GraylogNodeModule(configuration);
    }

    @Override
    protected List<Module> getCommandBindings(final FeatureFlags featureFlags) {
        final List<Module> modules = Lists.newArrayList(nodeModule);
        modules.addAll(getNodeCommandBindings(featureFlags));
        return modules;
    }

    protected abstract @Nonnull List<Module> getNodeCommandBindings(final FeatureFlags featureFlags);

    @Override
    protected List<Object> getCommandConfigurationBeans() {
        final List<Object> configurationBeans = new ArrayList<>(nodeModule.getConfigurationBeans());
        configurationBeans.addAll(getNodeCommandConfigurationBeans());
        return configurationBeans;
    }

    protected abstract @Nonnull List<Object> getNodeCommandConfigurationBeans();

}
