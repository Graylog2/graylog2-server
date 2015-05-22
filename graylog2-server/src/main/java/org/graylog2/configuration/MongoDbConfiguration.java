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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.ValidatorMethod;
import com.github.joschi.jadconfig.converters.StringListConverter;
import com.github.joschi.jadconfig.validators.InetPortValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.google.common.base.Joiner;
import com.google.common.net.HostAndPort;
import com.mongodb.MongoClientOptions;
import com.mongodb.MongoClientURI;
import com.mongodb.ServerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

public class MongoDbConfiguration {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDbConfiguration.class);

    @Parameter("mongodb_useauth")
    @Deprecated
    private boolean useAuth = false;

    @Parameter("mongodb_user")
    @Deprecated
    private String user;

    @Parameter("mongodb_password")
    @Deprecated
    private String password;

    @Parameter("mongodb_database")
    @Deprecated
    private String database = "graylog2";

    @Parameter("mongodb_host")
    @Deprecated
    private String host = "127.0.0.1";

    @Parameter(value = "mongodb_port", validator = InetPortValidator.class)
    @Deprecated
    private int port = 27017;

    @Parameter(value = "mongodb_max_connections", validator = PositiveIntegerValidator.class)
    private int maxConnections = 1000;

    @Parameter(value = "mongodb_threads_allowed_to_block_multiplier", validator = PositiveIntegerValidator.class)
    private int threadsAllowedToBlockMultiplier = 5;

    @Parameter(value = "mongodb_replica_set", converter = StringListConverter.class)
    @Deprecated
    private List<String> replicaSet;

    @Parameter("mongodb_uri")
    private String uri = null;


    @Deprecated
    public boolean isUseAuth() {
        return useAuth;
    }

    @Deprecated
    public String getUser() {
        return user;
    }

    @Deprecated
    public String getPassword() {
        return password;
    }

    @Deprecated
    public String getDatabase() {
        return database;
    }

    @Deprecated
    public int getPort() {
        return port;
    }

    @Deprecated
    public String getHost() {
        return host;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getThreadsAllowedToBlockMultiplier() {
        return threadsAllowedToBlockMultiplier;
    }

    public String getUri() {
        return uri;
    }

    public MongoClientURI getMongoClientURI() {
        if(isNullOrEmpty(uri)) {
            final StringBuilder sb = new StringBuilder("mongodb://");

            if(isUseAuth()) {
                sb.append(getUser()).append(':').append(getPassword()).append('@');
            }

            final List<ServerAddress> replicas = getReplicaSet();
            if(replicas == null) {
                sb.append(getHost()).append(':').append(getPort());
            } else {
                Joiner.on(',').skipNulls().appendTo(sb, replicas);
            }

            sb.append('/').append(getDatabase());
            uri = sb.toString();
        }

        final MongoClientOptions.Builder mongoClientOptionsBuilder = MongoClientOptions.builder()
                .connectionsPerHost(getMaxConnections())
                .threadsAllowedToBlockForConnectionMultiplier(getThreadsAllowedToBlockMultiplier());

        return new MongoClientURI(uri, mongoClientOptionsBuilder);
    }

    @Deprecated
    public List<ServerAddress> getReplicaSet() {
        if (replicaSet == null || replicaSet.isEmpty()) {
            return null;
        }

        final List<ServerAddress> replicaServers = new ArrayList<>(replicaSet.size());
        for (String host : replicaSet) {
            try {
                final HostAndPort hostAndPort = HostAndPort.fromString(host)
                        .withDefaultPort(27017);
                replicaServers.add(new ServerAddress(
                        InetAddress.getByName(hostAndPort.getHostText()), hostAndPort.getPort()));
            } catch (IllegalArgumentException e) {
                LOG.error("Malformed mongodb_replica_set configuration.", e);
                return null;
            } catch (UnknownHostException e) {
                LOG.error("Unknown host in mongodb_replica_set", e);
                return null;
            }
        }

        return replicaServers;
    }

    @ValidatorMethod
    public void validate() throws ValidationException {
        if ((isNullOrEmpty(getHost()) || isNullOrEmpty(getDatabase()) || getReplicaSet() == null) && isNullOrEmpty(getUri())) {
            throw new ValidationException("Either mongodb_uri OR mongodb_host and mongodb_database must not be empty");
        }

        if (isNullOrEmpty(getUri())) {
            LOG.info("You're using deprecated configuration options for MongoDB. Please use mongodb_uri.");
            LOG.info("Suggested value for mongodb_uri = {}", getMongoClientURI());
        }

        if (isUseAuth() && (isNullOrEmpty(getUser()) || isNullOrEmpty(getPassword()))) {
            throw new ValidationException("mongodb_user and mongodb_password have to be set if mongodb_useauth is true");
        }
    }
}
