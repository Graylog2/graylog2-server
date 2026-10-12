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
package org.graylog2.cluster.nodes.mongodb;

import com.google.common.base.Splitter;
import com.google.common.net.HostAndPort;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.system.shutdown.GracefulShutdownHook;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog2.shared.utilities.StringUtils.f;

public class DefaultMongodbConnectionResolver implements MongodbConnectionResolver, GracefulShutdownHook {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultMongodbConnectionResolver.class);

    // Lower-cased, as MongoDB connection string option names are case-insensitive.
    private static final Set<String> OPTIONS_OVERRIDDEN_PER_NODE = Set.of("replicaset", "directconnection");

    private final MongoClientURI mongoClientURI;
    private final Map<String, MongoClient> mongoClients = new ConcurrentHashMap<>();


    @Inject
    public DefaultMongodbConnectionResolver(MongoDbConfiguration configuration, GracefulShutdownService shutdownService) {
        this.mongoClientURI = configuration.getMongoClientURI();
        shutdownService.register(DefaultMongodbConnectionResolver.this);
    }

    @Override
    public MongoClient resolve(String nodeName) {
        return mongoClients.computeIfAbsent(nodeName, k -> createClient(nodeName));
    }

    @Nonnull
    private MongoClient createClient(String nodeName) {
        LOG.info("Creating mongo client for {}", nodeName);
        return new MongoClient(new MongoClientURI(buildConnectionString(nodeName)));
    }

    /**
     * Builds a connection string for a direct connection to a single node, reusing the configured connection string.
     * Credentials, database and options are copied verbatim (still URL-encoded), as the MongoDB driver already
     * accepted them for the main connection. Decoding and re-encoding them is not safe: e.g. a ':' in the password
     * must stay encoded, values may contain an encoded '=' and options like readPreferenceTags may be repeated.
     * The original string is split the same way the MongoDB driver does it: hosts end at the first '/' after the
     * scheme, credentials end at the last '@' before it and the options follow the next '?'.
     */
    private String buildConnectionString(String nodeName) {
        final String uri = mongoClientURI.getURI();
        final String withoutScheme = uri.substring(uri.indexOf("://") + 3);
        final int pathStart = withoutScheme.indexOf('/');
        final String userAndHosts = pathStart < 0 ? withoutScheme : withoutScheme.substring(0, pathStart);
        final String pathAndOptions = pathStart < 0 ? "" : withoutScheme.substring(pathStart + 1);

        // Includes the trailing '@', if there are credentials at all
        final String userInfo = userAndHosts.substring(0, userAndHosts.lastIndexOf('@') + 1);

        final int optionsStart = pathAndOptions.indexOf('?');
        final String database = optionsStart < 0 ? pathAndOptions : pathAndOptions.substring(0, optionsStart);
        final String options = optionsStart < 0 ? "" : pathAndOptions.substring(optionsStart + 1);

        final String perNodeOptions = Stream.concat(
                        Splitter.onPattern("[&;]").omitEmptyStrings().splitToStream(options)
                                .filter(option -> !OPTIONS_OVERRIDDEN_PER_NODE.contains(optionName(option))),
                        Stream.of("directConnection=true"))
                .collect(Collectors.joining("&"));

        // HostAndPort keeps the brackets of IPv6 addresses
        return f("mongodb://%s%s/%s?%s", userInfo, HostAndPort.fromString(nodeName), database, perNodeOptions);
    }

    private static String optionName(String option) {
        final int separator = option.indexOf('=');
        return (separator < 0 ? option : option.substring(0, separator)).toLowerCase(Locale.ROOT);
    }

    @Override
    public void doGracefulShutdown() throws Exception {
        for (Map.Entry<String, MongoClient> entry : mongoClients.entrySet()) {
            final String nodeName = entry.getKey();
            final MongoClient client = entry.getValue();
            LOG.info("Closing mongo client for {}", nodeName);
            try {
                client.close();
            } catch (Exception e) {
                LOG.warn("Error while closing mongo client for {}", nodeName, e);
            }
        }
        mongoClients.clear();
    }
}
