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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.junit.jupiter.MockitoExtension;

import java.nio.file.Files;
import java.nio.file.Path;
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
    public void startCommonNode(@TempDir Path tmpDir) throws Exception {
        final var configFile = tmpDir.resolve("common-node.conf");
        Files.write(configFile, List.of(
                "node_id_file = " + tmpDir.resolve("node-id").toAbsolutePath(),
                "password_secret = 1234567890123456"
        ));
        CommonNode node = spy(new CommonNode(configFile));
        node.run();
        verify(node, times(1)).startCommand();
    }

    static class CommonNode extends AbstractNodeCommand {

        public CommonNode(Path configFile) {
            super(new CommonNodeConfiguration() {
                @Parameter("password_secret")
                String passwordSecret;
                @Parameter("node_id_file")
                String nodeIdFile;
            });
            setConfigFile(configFile.toString());
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
