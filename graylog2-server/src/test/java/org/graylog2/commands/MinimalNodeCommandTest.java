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

import com.github.joschi.jadconfig.Parameter;
import com.google.inject.Module;
import jakarta.annotation.Nonnull;
import org.graylog2.GraylogNodeConfiguration;
import org.graylog2.featureflag.FeatureFlags;
import org.graylog2.plugin.ServerStatus;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.Set;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
public class MinimalNodeCommandTest {

    @Test
    public void startMinimalNode() {
        MinimalNode node = spy(new MinimalNode());
        node.run();
        verify(node, times(1)).startCommand();
    }

    static class MinimalNode extends AbstractNodeCommand {

        public MinimalNode() {
            super(new MinimalNodeConfiguration());
            URL resource = this.getClass().getResource("minimal-node.conf");
            if (resource == null) {
                Assertions.fail("Cannot read configuration file");
            }
            try {
                setConfigFile(resource.toURI().getPath());
            } catch (URISyntaxException e) {
                Assertions.fail("Cannot read configuration file");
            }
        }

        @Override
        protected @Nonnull List<Module> getNodeCommandBindings(FeatureFlags featureFlags) {
            return List.of();
        }

        @Override
        protected @Nonnull List<Object> getNodeCommandConfigurationBeans() {
            return List.of();
        }

        @Override
        protected void startCommand() {
        }

        static class MinimalNodeConfiguration implements GraylogNodeConfiguration {
            @Parameter("password_secret")
            String passwordSecret;

            @Override
            public boolean withPlugins() {
                return false;
            }

            @Override
            public boolean withEventBus() {
                return false;
            }

            @Override
            public boolean withScheduler() {
                return false;
            }

            @Override
            public boolean withMongoDb() {
                return false;
            }

            @Override
            public boolean withNodeIdFile() {
                return false;
            }

            @Override
            public Set<ServerStatus.Capability> withCapabilities() {
                return Set.of();
            }

            @Override
            public String getEnvironmentVariablePrefix() {
                return "MINIMAL_";
            }

            @Override
            public String getSystemPropertyPrefix() {
                return "minimal.";
            }

            @Override
            public boolean isMessageRecordingsEnabled() {
                return false;
            }

            @Override
            public boolean withInputs() {
                return false;
            }
        }
    }


}
