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
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static java.util.Objects.requireNonNull;

/**
 * This rule starts a MongoDB instance and provides a configured {@link org.graylog2.database.MongoConnection}.
 * <p>
 * Example usage:
 * <pre>{@code
 *   @Rule
 *   public final MongoDBInstance mongodb1 = MongoDBInstance.createForClass();
 *
 *   @Rule
 *   public final MongoDBInstance mongodb2 = MongoDBInstance.createForEachTest();
 * }</pre>
 */
public class MongoDBInstance extends ExternalResource implements AutoCloseable {
    public enum Lifecycle {
        METHOD, CLASS;
    }

    private static final Logger LOG = LoggerFactory.getLogger(MongoDBInstance.class);
    private static final String DEFAULT_IMAGE = "mongo";
    private static final String DEFAULT_VERSION = "3.6";
    private static final String DEFAULT_DATABASE_NAME = "graylog";
    private static final String DEFAULT_INSTANCE_NAME = "default";

    private static final int MONGODB_PORT = 27017;
    ;
    private static final String NETWORK_ALIAS = "mongodb";

    private static final ConcurrentMap<String, GenericContainer> CACHED_CONTAINER = new ConcurrentHashMap<>();

    private final String instanceName;
    private final Lifecycle lifecycle;
    private final String databaseName;
    private final GenericContainer container;

    private MongoConnection mongoConnection;
    private MongoDBFixtureImporter fixtureImporter;

    /**
     * Creates a new MongoDB instance for every test method.
     *
     * @return the MongoDB instance
     */
    public static MongoDBInstance createForEachTest() {
        return createWithDefaults(Network.newNetwork(), Lifecycle.METHOD);
    }

    /**
     * Creates a new MongoDB instance that is shared for all test methods in a test class.
     *
     * @return the MongoDB instance
     */
    public static MongoDBInstance createForClass() {
        return createWithDefaults(Network.newNetwork(), Lifecycle.CLASS);
    }

    public static MongoDBInstance createWithDefaults(Network network, Lifecycle lifecycle) {
        return new MongoDBInstance(DEFAULT_INSTANCE_NAME, lifecycle, DEFAULT_VERSION, DEFAULT_DATABASE_NAME, network);
    }

    private MongoDBInstance(String instanceName, Lifecycle lifecycle, String version, String databaseName, Network network) {
        this.instanceName = instanceName;
        this.lifecycle = lifecycle;
        this.databaseName = databaseName;

        switch (lifecycle) {
            case CLASS:
                this.container = CACHED_CONTAINER.computeIfAbsent(instanceName, k -> createContainer(version, network));
                break;
            case METHOD:
                this.container = createContainer(version, network);
                break;
            default:
                throw new IllegalArgumentException("Support for lifecycle " + lifecycle.toString() + " not implemented yet");
        }
    }

    private GenericContainer createContainer(String version, Network network) {
        return new GenericContainer<>(String.format(Locale.US, "%s:%s", DEFAULT_IMAGE, version))
                .withExposedPorts(MONGODB_PORT)
                .withNetwork(network)
                .withNetworkAliases(NETWORK_ALIAS)
                .waitingFor(Wait.forListeningPort());
    }

    public String instanceName() {
        return instanceName;
    }

    /**
     * Returns the established {@link MongoConnection} object.
     *
     * @return the connection object
     */
    public MongoConnection mongoConnection() {
        return requireNonNull(mongoConnection, "mongoConnection not initialized yet");
    }

    /**
     * Returns the IP address of the database instance.
     *
     * @return the IP address
     */
    public String ipAddress() {
        return container.getContainerIpAddress();
    }

    /**
     * Returns the port of the database instance.
     *
     * @return the port
     */
    public int port() {
        return container.getFirstMappedPort();
    }

    /**
     * Returns the database name.
     *
     * @return the database name
     */
    public String databaseName() {
        return databaseName;
    }

    /**
     * Drops the configured database.
     */
    public void dropDatabase() {
        LOG.debug("Dropping database {}", databaseName());
        mongoConnection().getMongoDatabase().drop();
    }

    @Override
    protected void before() {
        startContainer();
    }

    public void startContainer() {
        LOG.debug("Attempting to start container for image: {}", container.getDockerImageName());

        container.start();
        LOG.debug("Started container: {}", containerInfoString());

        final MongoDbConfiguration mongoConfiguration = new MongoDbConfiguration();
        mongoConfiguration.setUri(uri());

        this.mongoConnection = new MongoConnectionImpl(mongoConfiguration);
        this.mongoConnection.connect();
        this.mongoConnection.getMongoDatabase().drop();

        if (fixtureImporter != null) {
            fixtureImporter.importResources(mongoConnection().getMongoDatabase());
        }
    }

    private String uri() {
        return uriWithHostAndPort(ipAddress(), port());
    }

    public String internalUri() {
        return uriWithHostAndPort(NETWORK_ALIAS, MONGODB_PORT);
    }

    private String uriWithHostAndPort(String hostname, int port) {
        return String.format(Locale.US, "mongodb://%s:%d/%s", hostname, port, databaseName());
    }

    @Override
    public Statement apply(Statement base, Description description) {
        if (description.getMethodName() != null) {
            final MongoDBFixtures fixtureFiles = description.getAnnotation(MongoDBFixtures.class);
            if (fixtureFiles != null) {
                LOG.debug("Loading fixtures {} for {}#{}()", fixtureFiles.value(), description.getTestClass().getCanonicalName(), description.getMethodName());
                this.fixtureImporter = new MongoDBFixtureImporter(fixtureFiles.value(), description.getTestClass());
            }
        }
        return super.apply(base, description);
    }

    @Override
    protected void after() {
        dropDatabase();
        switch (lifecycle) {
            case CLASS:
                break;
            case METHOD:
                close();
                break;
        }
    }

    /**
     * Stops the database instance.
     */
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
