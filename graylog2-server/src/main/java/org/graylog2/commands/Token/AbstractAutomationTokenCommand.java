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
package org.graylog2.commands.Token;

import com.github.joschi.jadconfig.Parameter;
import com.google.inject.Module;
import jakarta.annotation.Nonnull;
import java.util.List;
import org.graylog2.Configuration;
import org.graylog2.bindings.ConfigurationModule;
import org.graylog2.cluster.ClusterConfigServiceImpl;
import org.graylog2.commands.AbstractNodeCommand;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.plugin.cluster.ClusterConfigService;

public abstract class AbstractAutomationTokenCommand extends AbstractNodeCommand {
    public AbstractAutomationTokenCommand(String commandName) {
        super(commandName, new TokenCommandConfiguration());
    }

    @Override
    protected @Nonnull List<Module> getNodeCommandBindings(final FeatureFlags featureFlags) {
        final TokenCommandConfiguration tokenCommandConfiguration = (TokenCommandConfiguration) configuration;
        return List.of(binder -> {
            binder.install(new ConfigurationModule(tokenCommandConfiguration));
            binder.bind(ClusterConfigService.class).to(ClusterConfigServiceImpl.class).asEagerSingleton();
        });
    }

    @Override
    protected @Nonnull List<Object> getNodeCommandConfigurationBeans() {
        return List.of();
    }

    private static class TokenCommandConfiguration extends Configuration {
        @Parameter("password_secret")
        String passwordSecret;

        @Override
        public boolean withPlugins() {
            return false;
        }

        @Override
        public boolean withInputs() {
            return false;
        }

    }
}
