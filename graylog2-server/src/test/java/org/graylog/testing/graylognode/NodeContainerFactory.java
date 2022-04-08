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
import org.testcontainers.containers.Container;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.HttpWaitStrategy;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.containers.wait.strategy.WaitAllStrategy;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.utility.MountableFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermissions;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.time.temporal.ChronoUnit.SECONDS;
import static org.graylog.testing.ResourceUtil.resourceToTmpFile;
import static org.graylog.testing.graylognode.NodeContainerConfig.API_PORT;
import static org.graylog.testing.graylognode.NodeContainerConfig.DEBUG_PORT;

public class NodeContainerFactory {
    private static final Logger LOG = LoggerFactory.getLogger(NodeContainerFactory.class);
    private static final String FEATURE_PREFIX = "GRAYLOG_FEATURE";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @SuppressWarnings("OctalInteger")
    private static final int EXECUTABLE_MODE = 0100755;
    // sha2 for password "admin"
    private static final String ADMIN_PW_SHA2 = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918";

    private static final String GRAYLOG_HOME = "/usr/share/graylog";

    public static GenericContainer<?> buildContainer(NodeContainerConfig config) {
        checkBinaries(config);
        if (!config.skipPackaging) {
            MavenPackager.packageJarIfNecessary(config);
        } else {
            LOG.info("Skipping packaging");
        }
        ImageFromDockerfile image = createImage(config);

        return createRunningContainer(config, image);
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

    private static boolean containerFileExists(final GenericContainer container, String path) {
        try {
            Container.ExecResult r = container.execInContainer("/bin/sh", "-c",
                    "if [ -f " + path + " ] ; then echo '0' ; else (>&2 echo '1') ; fi");

            return !r.getStderr().contains("1");
        } catch (IOException | InterruptedException e) {
            LOG.error("Could not check for file existence: " + path, e);
            return false;
        }
    }

    private static void checkBinaries(NodeContainerConfig config) {
        final Path fileCopyBaseDir = config.mavenProjectDirProvider.getFileCopyBaseDir();
        config.mavenProjectDirProvider.getFilesToAddToBinDir().forEach(filename -> {
            Path path = fileCopyBaseDir.resolve(filename);
            if (!Files.exists(path)) {
                LOG.error("Mandatory file {} does not exist in {}", filename, fileCopyBaseDir);
            } else if (!Files.isExecutable(path)) {
                LOG.warn("File {} in {} is not executable, setting executable flag.", filename, fileCopyBaseDir);
                try {
                    Files.setPosixFilePermissions(path, PosixFilePermissions.fromString("rwxrwxr-x"));
                } catch (IOException iox) {
                    LOG.error("Setting executable flag for file " + filename + " in " + fileCopyBaseDir + " failed.", iox);
                }
            }
        });
    }

    private static GenericContainer<?> createRunningContainer(NodeContainerConfig config, ImageFromDockerfile image) {
        Path fileCopyBaseDir = config.mavenProjectDirProvider.getFileCopyBaseDir();
        List<Path> pluginJars = config.pluginJarsProvider.getJars();
        boolean includeFrontend = config.mavenProjectDirProvider.includeFrontend();

        GenericContainer<?> container = new GenericContainer<>(image)
                .withFileSystemBind(property("server_jar"), GRAYLOG_HOME + "/graylog.jar", BindMode.READ_ONLY)
                .withNetwork(config.network)
                .withEnv("GRAYLOG_MONGODB_URI", config.mongoDbUri)
                .withEnv("GRAYLOG_ELASTICSEARCH_HOSTS", config.elasticsearchUri)
                // TODO: should we set this override search version or let graylog server to detect it from the search server itself?
                .withEnv("GRAYLOG_ELASTICSEARCH_VERSION", config.elasticsearchVersion.encode())
                .withEnv("GRAYLOG_PASSWORD_SECRET", "M4lteserKreuzHerrStrack?")
                .withEnv("GRAYLOG_NODE_ID_FILE", "data/config/node-id")
                .withEnv("GRAYLOG_HTTP_BIND_ADDRESS", "0.0.0.0:" + API_PORT)
                .withEnv("GRAYLOG_ROOT_PASSWORD_SHA2", ADMIN_PW_SHA2)
                .withEnv("GRAYLOG_LB_RECOGNITION_PERIOD_SECONDS", "0")
                .withEnv("GRAYLOG_VERSIONCHECKS", "false")

                .withEnv("GRAYLOG_TRANSPORT_EMAIL_ENABLED", "true")
                .withEnv("GRAYLOG_TRANSPORT_EMAIL_HOSTNAME", "mailserver")
                .withEnv("GRAYLOG_TRANSPORT_EMAIL_PORT", "1025")
                .withEnv("GRAYLOG_TRANSPORT_EMAIL_USE_AUTH", "false")
                .withEnv("GRAYLOG_TRANSPORT_EMAIL_SUBJECT_PREFIX", "[graylog]")
                .withEnv("GRAYLOG_TRANSPORT_EMAIL_FROM_EMAIL", "developers@graylog.com")

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

        if (!includeFrontend) {
            container.withEnv("DEVELOPMENT", "true");
        }

        pluginJars.forEach(hostPath -> {
            if (Files.exists(hostPath)) {
                final Path containerPath = Paths.get(GRAYLOG_HOME, "plugin", hostPath.getFileName().toString());
                container.addFileSystemBind(hostPath.toString(), containerPath.toString(), BindMode.READ_ONLY);
            }
        });

        addEnabledFeatureFlagsToContainerEnv(config, container);

        container.start();

        if (config.enableDebugging) {
            LOG.info("Container debug port: " + container.getMappedPort(DEBUG_PORT));
        }

        config.mavenProjectDirProvider.getFilesToAddToBinDir().forEach(filename -> {
            final Path originalPath = fileCopyBaseDir.resolve(filename);
            final String containerPath = GRAYLOG_HOME + "/bin/" + originalPath.getFileName();
            container.copyFileToContainer(MountableFile.forHostPath(originalPath), containerPath);
            if (!containerFileExists(container, containerPath)) {
                LOG.error("Mandatory file {} does not exist in container at {}", filename, containerPath);
            }
            // fix to duplicate files for Docker on OSX, system.arch is aarch64 instead or arm64
            if(filename.endsWith("_arm64")) {
                final String aarch64Path = containerPath.replaceAll("_arm64", "_aarch64");
                container.copyFileToContainer(MountableFile.forHostPath(originalPath), aarch64Path);
                if (!containerFileExists(container, aarch64Path)) {
                    LOG.error("Mandatory file {} does not exist in container at {}", filename, aarch64Path);
                }

            }
        });

        return container;
    }

    private static void addEnabledFeatureFlagsToContainerEnv(NodeContainerConfig config, GenericContainer<?> container) {
        List<String> prefixed = config.getEnabledFeatureFlags()
                .stream()
                .map(e -> e.startsWith(FEATURE_PREFIX) ? e : String.join("_", FEATURE_PREFIX, e))
                .map(e -> e.toUpperCase(Locale.ENGLISH))
                .collect(Collectors.toList());

        for (String name : prefixed) {
            container.withEnv(name, "on");
        }
    }

    private static Path pathTo(String propertyName) {
        return Paths.get(property(propertyName));
    }

    private static String property(String propertyName) {
        return PropertyLoader.get("api-it-tests.properties", propertyName);
    }
}
