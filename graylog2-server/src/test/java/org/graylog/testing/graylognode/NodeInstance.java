/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.testing.graylognode;

import com.google.common.io.Resources;
import org.graylog.testing.PropertyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;

public class NodeInstance {

    private static final Logger LOG = LoggerFactory.getLogger(NodeInstance.class);

    @SuppressWarnings("OctalInteger")
    private static final int EXECUTABLE_MODE = 0100755;

    private final GenericContainer container;

    public static NodeInstance createStarted(Network network, String mongoDbUri, String elasticsearchUri) {
        NodeInstance instance = new NodeInstance(network, mongoDbUri, elasticsearchUri);
        instance.container.start();
        return instance;
    }

    public NodeInstance(Network network, String mongoDbUri, String elasticsearchUri) {
        this.container = buildContainer(network, mongoDbUri, elasticsearchUri);
    }

    private GenericContainer buildContainer(Network network, String mongoDbUri, String elasticsearchUri) {

        packageServerIfExecutedFromIntellij();

        File entrypointScript = resourceFile("org/graylog/testing/graylognode/docker-entrypoint.sh");

        final ImageFromDockerfile image = new ImageFromDockerfile()
                .withFileFromClasspath("Dockerfile", "org/graylog/testing/graylognode/Dockerfile")
                .withFileFromFile("docker-entrypoint.sh", entrypointScript, EXECUTABLE_MODE)
                .withFileFromClasspath("graylog.conf", "org/graylog/testing/graylognode/config/graylog.conf")
                .withFileFromClasspath("log4j2.xml", "org/graylog/testing/graylognode/config/log4j2.xml")
                .withFileFromPath("sigar", pathTo("sigar_dir"))
                .withFileFromPath("graylog.jar", pathTo("server_jar"))
                .withFileFromPath("graylog-plugin-aws.jar", pathTo("aws_plugin_jar"))
                .withFileFromPath("graylog-plugin-threatintel.jar", pathTo("threatintel_plugin_jar"))
                .withFileFromPath("graylog-plugin-collector.jar", pathTo("collector_plugin_jar"));

        return new GenericContainer<>(image)
                .withExposedPorts(9000)
                .withNetwork(network)
                .withEnv("GRAYLOG_MONGODB_URI", mongoDbUri)
                .withEnv("GRAYLOG_ELASTICSEARCH_HOSTS", elasticsearchUri)
                .waitingFor(Wait.forHttp("/api"));
    }

    private void packageServerIfExecutedFromIntellij() {
        if (isRunFromIntellij()) {
            LOG.info("Running from Intellij. Packaging server jar now...");
            MavenPackager.packageJar(property("project_dir") + "/..");
        }
    }

    //It would be more robust to detect whether tests are executed from within maven and skip if yes
    private boolean isRunFromIntellij() {
        String classPath = System.getProperty("java.class.path");
        return classPath != null && classPath.split(":")[0].endsWith("idea_rt.jar");
    }

    @SuppressWarnings("UnstableApiUsage")
    private File resourceFile(@SuppressWarnings("SameParameterValue") String resourceName) {
        try {
            return new File(Resources.getResource(resourceName).toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private Path pathTo(String propertyName) {
        return Paths.get(property(propertyName));
    }

    private String property(String propertyName) {
        return PropertyLoader.get("api-it-tests.properties", propertyName);
    }

    public void stop() {
        container.stop();
    }

    public String getApiAddress() {
        return String.format(Locale.US, "http://%s:%d/api", container.getContainerIpAddress(), container.getFirstMappedPort());
    }
}
