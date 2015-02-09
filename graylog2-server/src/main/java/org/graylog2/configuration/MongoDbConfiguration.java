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
import com.google.common.net.HostAndPort;
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

    @Parameter(value = "mongodb_useauth", required = true)
    private boolean useAuth = false;

    @Parameter(value = "mongodb_user")
    private String user;

    @Parameter(value = "mongodb_password")
    private String password;

    @Parameter(value = "mongodb_database", required = true)
    private String database = "graylog2";

    @Parameter(value = "mongodb_host", required = true)
    private String host = "127.0.0.1";

    @Parameter(value = "mongodb_port", required = true, validator = InetPortValidator.class)
    private int port = 27017;

    @Parameter(value = "mongodb_max_connections", validator = PositiveIntegerValidator.class)
    private int maxConnections = 1000;

    @Parameter(value = "mongodb_threads_allowed_to_block_multiplier", validator = PositiveIntegerValidator.class)
    private int threadsAllowedToBlockMultiplier = 5;

    @Parameter(value = "mongodb_replica_set", converter = StringListConverter.class)
    private List<String> replicaSet;


    public boolean isUseAuth() {
        return useAuth;
    }

    public String getUser() {
        return user;
    }

    public String getPassword() {
        return password;
    }

    public String getDatabase() {
        return database;
    }

    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public int getMaxConnections() {
        return maxConnections;
    }

    public int getThreadsAllowedToBlockMultiplier() {
        return threadsAllowedToBlockMultiplier;
    }


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
        if (isUseAuth() && (isNullOrEmpty(getUser()) || isNullOrEmpty(getPassword()))) {
            throw new ValidationException("mongodb_user and mongodb_password have to be set if mongodb_useauth is true");
        }
    }
}
