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
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.CommonNodeConfiguration;
import org.graylog2.featureflag.FeatureFlags;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@ExtendWith(MongoDBExtension.class)
public class CommonNodeCommandTest {

    static MongoDBTestService mongodb;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        System.setProperty("graylog.mongodb_uri", mongodb.uri());
        CommonNodeCommandTest.mongodb = mongodb;
    }

    @Test
    public void startCommonNode() {
        CommonNode node = spy(new CommonNode());
        node.run();
        verify(node, times(1)).startCommand();
    }

    static class CommonNode extends AbstractNodeCommand {

        public CommonNode() {
            super(new CommonNodeConfiguration() {
                @Parameter("password_secret")
                String passwordSecret;
                @Parameter("node_id_file")
                String nodeIdFile;
            });
            URL resource = this.getClass().getResource("common-node.conf");
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


    }


}
