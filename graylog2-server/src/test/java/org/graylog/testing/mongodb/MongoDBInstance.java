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
package org.graylog.testing.mongodb;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.MongoConnectionImpl;
import org.junit.rules.ExternalResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Locale;

import static java.util.Objects.requireNonNull;

/**
 * This rule starts a MongoDB instance and provides a configured {@link org.graylog2.database.MongoConnection}.
 */
public class MongoDBInstance extends ExternalResource implements AutoCloseable {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDBInstance.class);
    private static final String DEFAULT_IMAGE = "mongo";
    private static final String DEFAULT_VERSION = "3.6";
    private static final String DEFAULT_DATABASE_NAME = "graylog";

    private final String databaseName;
    private final GenericContainer container;
    private MongoConnection mongoConnection;

    public static MongoDBInstance create() {
        return new MongoDBInstance(DEFAULT_VERSION, DEFAULT_DATABASE_NAME);
    }

    private MongoDBInstance(String version, String databaseName) {
        this.databaseName = databaseName;
        this.container = new GenericContainer<>(String.format(Locale.US, "%s:%s", DEFAULT_IMAGE, version))
                .withExposedPorts(27017)
                .waitingFor(Wait.forListeningPort());
    }

    public MongoConnection mongoConnection() {
        return requireNonNull(mongoConnection, "mongoConnection not initialized yet");
    }

    public String ipAddress() {
        return container.getContainerIpAddress();
    }

    public String database() {
        return databaseName;
    }

    public int port() {
        return container.getFirstMappedPort();
    }

    public void dropDatabase() {
        LOG.debug("Dropping database {}", database());
        mongoConnection().getMongoDatabase().drop();
    }

    @Override
    protected void before() {
        LOG.debug("Attempting to start container for image: {}", container.getDockerImageName());
        container.start();
        LOG.debug("Started container: {}", containerInfoString());

        final MongoDbConfiguration mongoConfiguration = new MongoDbConfiguration();
        mongoConfiguration.setUri(String.format(Locale.US, "mongodb://%s:%d/%s",
                ipAddress(), port(), database()));

        this.mongoConnection = new MongoConnectionImpl(mongoConfiguration);
        this.mongoConnection.connect();
        this.mongoConnection.getMongoDatabase().drop();
    }

    @Override
    protected void after() {
        close();
    }

    @Override
    public void close() {
        if (container != null) {
            LOG.debug("Stopping container: {}", containerInfoString());
            container.close();
        }
    }

    private String containerInfoString() {
        final InspectContainerResponse containerInfo = container.getContainerInfo();
        return String.format(Locale.US, "%s%s/%s", containerInfo.getId(), containerInfo.getName(), containerInfo.getConfig().getImage());
    }
}
