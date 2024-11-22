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
import org.graylog2.CommonNodeConfiguration;
import org.graylog2.featureflag.FeatureFlags;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.fail;

public class MinimalNode extends AbstractNodeCommand {

    public MinimalNode() {
        super(new MinimalNodeConfiguration());
        URL resource = this.getClass().getResource("minimal-node.conf");
        if (resource == null) {
            fail("Cannot read configuration file");
        }
        try {
            setConfigFile(resource.toURI().getPath());
        } catch (URISyntaxException e) {
            fail("Cannot read configuration file");
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

    static class MinimalNodeConfiguration implements CommonNodeConfiguration {
        @Parameter("password_secret")
        String passwordSecret;

        @Parameter("node_id_file")
        String nodeIdFile;

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
    }
}



