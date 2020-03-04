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

import com.google.common.base.Stopwatch;
import org.apache.commons.io.FileUtils;
import org.graylog.testing.PropertyLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.BindMode;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class NodeInstance {

    private static final Logger LOG = LoggerFactory.getLogger(NodeInstance.class);

    @SuppressWarnings("OctalInteger")
    private static final int EXECUTABLE_MODE = 0100755;
    private static final String ADMIN_PW_SHA2 = "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918";

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
        MavenPackager.packageJarIfNecessary(property("server_project_dir"));

        File entrypointScript = resourceFile("org/graylog/testing/graylognode/docker-entrypoint.sh");

        final ImageFromDockerfile image = new ImageFromDockerfile()
                .withFileFromClasspath("Dockerfile", "org/graylog/testing/graylognode/Dockerfile")
                // set mode here explicitly, because file system permissions can get lost when executing from maven
                .withFileFromFile("docker-entrypoint.sh", entrypointScript, EXECUTABLE_MODE)
                .withFileFromPath("graylog.conf", pathTo("graylog_config"))
                .withFileFromClasspath("log4j2.xml", "log4j2.xml")
                .withFileFromPath("sigar", pathTo("sigar_dir"));

        String graylogHome = "/usr/share/graylog";

        return new GenericContainer<>(image)
                .withFileSystemBind(property("server_jar"), graylogHome + "/graylog.jar", BindMode.READ_ONLY)
                .withFileSystemBind(property("aws_plugin_jar"), graylogHome + "/plugin/graylog-plugin-aws.jar", BindMode.READ_ONLY)
                .withFileSystemBind(property("threatintel_plugin_jar"), graylogHome + "/plugin/graylog-plugin-threatintel.jar", BindMode.READ_ONLY)
                .withFileSystemBind(property("collector_plugin_jar"), graylogHome + "/plugin/graylog-plugin-collector.jar", BindMode.READ_ONLY)
                .withExposedPorts(9000)
                .withNetwork(network)
                .withEnv("GRAYLOG_MONGODB_URI", mongoDbUri)
                .withEnv("GRAYLOG_ELASTICSEARCH_HOSTS", elasticsearchUri)
                .withEnv("GRAYLOG_PASSWORD_SECRET", "M4lteserKreuzHerrStrack?")
                .withEnv("GRAYLOG_NODE_ID_FILE", "data/config/node-id")
                .withEnv("GRAYLOG_HTTP_BIND_ADDRESS", "0.0.0.0:9000")
                .withEnv("GRAYLOG_ROOT_PASSWORD_SHA2", ADMIN_PW_SHA2)
                .waitingFor(Wait.forHttp("/api"));
    }

    // workaround for testcontainers which only allows passing permissions if you pass a `File`
    private File resourceFile(@SuppressWarnings("SameParameterValue") String resourceName) {

        InputStream resource = this.getClass().getClassLoader().getResourceAsStream(resourceName);

        if (resource == null)
            throw new RuntimeException("Couldn't load resource " + resourceName);

        File f = new File("/tmp/" + UUID.randomUUID().toString() + "-" + Paths.get(resourceName).getFileName());

        try {
            FileUtils.copyInputStreamToFile(resource, f);
        } catch (IOException e) {
            throw new RuntimeException("Error copying resource to file: " + resourceName);
        }

        return f;
    }

    private Path pathTo(String propertyName) {
        return Paths.get(property(propertyName));
    }

    private static String property(String propertyName) {
        return PropertyLoader.get("api-it-tests.properties", propertyName);
    }

    public void restart() {
        Stopwatch sw = Stopwatch.createStarted();
        container.stop();
        container.start();
        sw.stop();
        LOG.info("Restarted node container in " + sw.elapsed(TimeUnit.SECONDS));
    }

    public String getApiAddress() {
        return String.format(Locale.US, "http://%s:%d/api", container.getContainerIpAddress(), container.getFirstMappedPort());
    }
}
