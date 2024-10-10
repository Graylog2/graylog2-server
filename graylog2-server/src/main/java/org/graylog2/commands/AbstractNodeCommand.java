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
import com.google.inject.Binder;
import com.google.inject.Module;
import org.graylog2.CommonNodeConfiguration;
import org.graylog2.bindings.GraylogNodeModule;
import org.graylog2.bootstrap.CmdLineTool;
import org.graylog2.configuration.PathConfiguration;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.featureflag.FeatureFlags;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.List;

/**
 * Inherit from this command to create new standalone node types.
 */
public abstract class AbstractNodeCommand extends CmdLineTool<CommonNodeConfiguration> {

    private final GraylogNodeModule nodeModule;

    public AbstractNodeCommand(final CommonNodeConfiguration configuration) {
        this(null, configuration);
    }

    public AbstractNodeCommand(final String commandName, final CommonNodeConfiguration configuration) {
        super(commandName, configuration);
        this.nodeModule = new GraylogNodeModule(configuration);
    }

    @Override
    protected List<Module> getCommandBindings(final FeatureFlags featureFlags) {
        final List<Module> modules = Lists.newArrayList(
                Binder::requireExplicitBindings,
                nodeModule
        );
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

    @Override
    protected void beforeStart(TLSProtocolsConfiguration tlsProtocolsConfiguration, PathConfiguration pathConfiguration) {
        super.beforeStart(tlsProtocolsConfiguration, pathConfiguration);

        // This needs to run before the first SSLContext is instantiated,
        // because it sets up the default SSLAlgorithmConstraints
        applySecuritySettings(tlsProtocolsConfiguration);

        // Set these early in the startup because netty's NativeLibraryUtil uses a static initializer
        setNettyNativeDefaults(pathConfiguration);
    }

    private void setNettyNativeDefaults(PathConfiguration pathConfiguration) {
        // Give netty a better spot than /tmp to unpack its tcnative libraries
        if (System.getProperty("io.netty.native.workdir") == null) {
            System.setProperty("io.netty.native.workdir", pathConfiguration.getNativeLibDir()
                    .toAbsolutePath()
                    .toString());
        }
        // Don't delete the native lib after unpacking, as this confuses needrestart(1) on some distributions
        if (System.getProperty("io.netty.native.deleteLibAfterLoading") == null) {
            System.setProperty("io.netty.native.deleteLibAfterLoading", "false");
        }
    }
}
