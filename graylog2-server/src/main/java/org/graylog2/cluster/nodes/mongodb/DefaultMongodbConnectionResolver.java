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

import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.common.net.HostAndPort;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.MongoCredential;
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import org.graylog2.configuration.MongoDbConfiguration;
import org.graylog2.system.shutdown.GracefulShutdownHook;
import org.graylog2.system.shutdown.GracefulShutdownService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog2.shared.utilities.StringUtils.f;

public class DefaultMongodbConnectionResolver implements MongodbConnectionResolver, GracefulShutdownHook {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultMongodbConnectionResolver.class);

    private static final String SRV_SCHEME_PREFIX = "mongodb+srv://";

    // The per-node clients only run occasional cluster overview commands, so they don't need the configured pool size,
    // which is sized for the main connection.
    private static final int PER_NODE_MAX_POOL_SIZE = 5;

    // Lower-cased, as MongoDB connection string option names are case-insensitive. Besides the options the per-node
    // connection overrides, these are the options only allowed in mongodb+srv:// connection strings, as the per-node
    // connection strings are mongodb:// ones.
    private static final Set<String> OPTIONS_DROPPED_PER_NODE = Set.of("replicaset", "directconnection", "maxpoolsize",
            "srvmaxhosts", "srvservicename", "srvallowedhostssuffix");

    private final MongoDbConfiguration configuration;
    private final Map<String, MongoClient> mongoClients = new ConcurrentHashMap<>();


    @Inject
    public DefaultMongodbConnectionResolver(MongoDbConfiguration configuration, GracefulShutdownService shutdownService) {
        // Don't parse the connection string here: for mongodb+srv:// that means DNS lookups
        this.configuration = configuration;
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
     * The original string is split the same way the MongoDB driver does it: hosts end at the first '/' or '?' after
     * the scheme, as the database path is optional even with options, credentials end at the last '@' before it and
     * the options follow the next '?'.
     */
    private String buildConnectionString(String nodeName) {
        final String uri = configuration.getUri();
        final String withoutScheme = uri.substring(uri.indexOf("://") + 3);
        final int hostsEnd = CharMatcher.anyOf("/?").indexIn(withoutScheme);
        final String userAndHosts = hostsEnd < 0 ? withoutScheme : withoutScheme.substring(0, hostsEnd);
        // Without the '/' separating the hosts from the database, but with the '?' of the options, if there's no path
        final String pathAndOptions = hostsEnd < 0 ? "" : withoutScheme.substring(withoutScheme.charAt(hostsEnd) == '/' ? hostsEnd + 1 : hostsEnd);

        // Includes the trailing '@', if there are credentials at all
        final String userInfo = userAndHosts.substring(0, userAndHosts.lastIndexOf('@') + 1);

        final int optionsStart = pathAndOptions.indexOf('?');
        final String database = optionsStart < 0 ? pathAndOptions : pathAndOptions.substring(0, optionsStart);
        final String options = optionsStart < 0 ? "" : pathAndOptions.substring(optionsStart + 1);

        final List<String> configuredOptions = Splitter.onPattern("[&;]").omitEmptyStrings().splitToList(options);
        final Set<String> configuredOptionNames = configuredOptions.stream()
                .map(DefaultMongodbConnectionResolver::optionName)
                .collect(Collectors.toSet());
        final Stream<String> srvImpliedOptions = uri.startsWith(SRV_SCHEME_PREFIX)
                ? srvImpliedOptions(!userInfo.isEmpty(), configuredOptionNames)
                : Stream.empty();

        final String perNodeOptions = Stream.of(
                        configuredOptions.stream().filter(option -> !OPTIONS_DROPPED_PER_NODE.contains(optionName(option))),
                        srvImpliedOptions,
                        Stream.of("directConnection=true", f("maxPoolSize=%d", PER_NODE_MAX_POOL_SIZE)))
                .flatMap(Function.identity())
                .collect(Collectors.joining("&"));

        // HostAndPort keeps the brackets of IPv6 addresses
        return f("mongodb://%s%s/%s?%s", userInfo, HostAndPort.fromString(nodeName), database, perNodeOptions);
    }

    /**
     * Options a mongodb+srv:// connection string implies, which have to be set explicitly in the mongodb:// per-node
     * connection string: TLS is enabled unless configured otherwise, and the DNS TXT record may define the authSource.
     */
    private Stream<String> srvImpliedOptions(boolean hasCredentials, Set<String> configuredOptionNames) {
        final Stream.Builder<String> implied = Stream.builder();
        if (!configuredOptionNames.contains("tls") && !configuredOptionNames.contains("ssl")) {
            implied.add("tls=true");
        }
        if (hasCredentials && !configuredOptionNames.contains("authsource")) {
            // Parsing resolves the DNS TXT record, so the credential has the effective authSource
            final MongoCredential credential = configuration.getMongoClientURI().getCredentials();
            if (credential != null && credential.getSource() != null) {
                // The MongoDB driver doesn't decode '+' to a space
                final String authSource = URLEncoder.encode(credential.getSource(), StandardCharsets.UTF_8).replace("+", "%20");
                implied.add(f("authSource=%s", authSource));
            }
        }
        return implied.build();
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
