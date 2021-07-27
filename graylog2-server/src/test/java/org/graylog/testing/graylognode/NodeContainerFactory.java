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
package org.graylog.testing.graylognode;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.testing.PropertyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import java.util.stream.StreamSupport;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.graylog.testing.ResourceUtil.resourceToTmpFile;
import static org.graylog.testing.graylognode.NodeContainerConfig.API_PORT;
import static org.graylog.testing.graylognode.NodeContainerConfig.DEBUG_PORT;

public class NodeContainerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(NodeContainerFactory.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SuppressWarnings("OctalInteger")
    private static final int EXECUTABLE_MODE = 0100755;
    // sha2 for password "admin"
    private static final String ADMIN_PW_SHA2 = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918";

    public static GenericContainer<?> buildContainer(NodeContainerConfig config, List<Path> pluginJars,
            Path mavenProjectDir) {
        if (!config.skipPackaging) {
            MavenPackager.packageJarIfNecessary(mavenProjectDir);
        } else {
            LOG.info("Skipping packaging");
        }
        ImageFromDockerfile image = createImage(config);

        return createRunningContainer(config, image, pluginJars);
    }

    private static ImageFromDockerfile createImage(NodeContainerConfig config) {
        // testcontainers only allows passing permissions if you pass a `File`
        File entrypointScript = resourceToTmpFile("org/graylog/testing/graylognode/docker-entrypoint.sh");

        // Don't delete the image after running the tests so we don't have to rebuild it every time...
        ImageFromDockerfile image = new ImageFromDockerfile("local/graylog-full-backend-test-server:latest", false)
                .withFileFromClasspath("Dockerfile", "org/graylog/testing/graylognode/Dockerfile")
                // set mode here explicitly, because file system permissions can get lost when executing from maven
                .withFileFromFile("docker-entrypoint.sh", entrypointScript, EXECUTABLE_MODE)
                .withFileFromPath("graylog.conf", pathTo("graylog_config"))
                .withFileFromClasspath("log4j2.xml", "log4j2.xml");
        if (config.enableDebugging) {
            image.withBuildArg("DEBUG_OPTS", "-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=0.0.0.0:5005");
        }
        return image;
    }

    private static GenericContainer<?> createRunningContainer(NodeContainerConfig config, ImageFromDockerfile image,
            List<Path> pluginJars) {
        String graylogHome = "/usr/share/graylog";

        GenericContainer<?> container = new GenericContainer<>(image)
                .withFileSystemBind(property("server_jar"), graylogHome + "/graylog.jar", BindMode.READ_ONLY)
                .withNetwork(config.network)
                .withEnv("DEVELOPMENT", "true")
                .withEnv("GRAYLOG_MONGODB_URI", config.mongoDbUri)
                .withEnv("GRAYLOG_ELASTICSEARCH_HOSTS", config.elasticsearchUri)
                .withEnv("GRAYLOG_ELASTICSEARCH_VERSION", config.elasticsearchVersion)
                .withEnv("GRAYLOG_PASSWORD_SECRET", "M4lteserKreuzHerrStrack?")
                .withEnv("GRAYLOG_NODE_ID_FILE", "data/config/node-id")
                .withEnv("GRAYLOG_HTTP_BIND_ADDRESS", "0.0.0.0:" + API_PORT)
                .withEnv("GRAYLOG_ROOT_PASSWORD_SHA2", ADMIN_PW_SHA2)
                .withEnv("GRAYLOG_LB_RECOGNITION_PERIOD_SECONDS", "0")
                .withEnv("GRAYLOG_VERSIONCHECKS", "false")
                .waitingFor(new WaitAllStrategy()
                        .withStrategy(Wait.forLogMessage(".*Graylog server up and running.*", 1))
                        // To be able to search for data we need the index ranges to be computed. Since this is an async
                        // background job, we need to wait until they have been created.
                        .withStrategy(new HttpWaitStrategy()
                                .forPort(API_PORT)
                                .forPath("/api/system/indices/ranges")
                                .withMethod("GET")
                                .withBasicCredentials("admin", "admin")
                                .forResponsePredicate(body -> {
                                    try {
                                        return StreamSupport.stream(OBJECT_MAPPER.readTree(body).path("ranges").spliterator(), false)
                                                // With the default configuration, there should be a least one "graylog_" prefixed index
                                                .anyMatch(range -> range.path("index_name").asText().startsWith("graylog_"));
                                    } catch (IOException e) {
                                        throw new RuntimeException("Couldn't extract response", e);
                                    }
                                })))
                .withExposedPorts(config.portsToExpose())
                .withStartupTimeout(Duration.of(120, SECONDS));

        pluginJars.forEach(hostPath -> {
            if (Files.exists(hostPath)) {
                final Path containerPath = Paths.get(graylogHome, "plugin", hostPath.getFileName().toString());
                container.addFileSystemBind(hostPath.toString(), containerPath.toString(), BindMode.READ_ONLY);
            }
        });

        container.start();

        if (config.enableDebugging) {
            LOG.info("Container debug port: " + container.getMappedPort(DEBUG_PORT));
        }
        return container;
    }

    private static Path pathTo(String propertyName) {
        return Paths.get(property(propertyName));
    }

    private static String property(String propertyName) {
        return PropertyLoader.get("api-it-tests.properties", propertyName);
    }
}
