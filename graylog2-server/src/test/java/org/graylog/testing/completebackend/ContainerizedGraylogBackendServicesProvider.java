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
package org.graylog.testing.completebackend;

import org.graylog.testing.containermatrix.MongodbServer;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.storage.SearchVersion;
import org.testcontainers.containers.Network;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Stream;

import static org.graylog.testing.completebackend.ContainerizedGraylogBackend.PASSWORD_SECRET;
import static org.graylog.testing.completebackend.ContainerizedGraylogBackend.ROOT_PASSWORD_SHA_2;

/**
 * Provides all services (searchDB, mongoDB, ...) that a {@link ContainerizedGraylogBackend} depends on.
 * Bundling these services in one class allows us to re-use them in multiple test runs
 * without the need to restart them every time.
 * The {@link ContainerizedGraylogBackend} using these services must run a {@link Services#cleanUp()}}
 * after it shuts down.
 */
public class ContainerizedGraylogBackendServicesProvider implements AutoCloseable {
    private final ConcurrentMap<String, Services> servicesCache;

    public ContainerizedGraylogBackendServicesProvider() {
        servicesCache = new ConcurrentHashMap<>();
    }

    public Services getServices(SearchVersion searchVersion, MongodbServer mongodbVersion, final boolean withMailServerEnabled, List<String> enabledFeatureFlags) {
        var lookupKey = Services.buildLookupKey(searchVersion, mongodbVersion, withMailServerEnabled, enabledFeatureFlags);
        return servicesCache.computeIfAbsent(lookupKey, (k) -> Services.create(searchVersion, mongodbVersion, withMailServerEnabled, enabledFeatureFlags));
    }

    @Override
    public void close() throws Exception {
        servicesCache.values().forEach(s -> {
            try {
                s.close();
            } catch (Exception ignored) {
            }
        });
    }

    public static class Services implements AutoCloseable {
        private final Network network;
        private final SearchServerInstance searchServerInstance;
        private final MongoDBInstance mongoDBInstance;
        private final MailServerContainer mailServerContainer;

        private static Services create(SearchVersion searchVersion, MongodbServer mongodbVersion, boolean withMailServerEnabled, List<String> enabledFeatureFlags) {
            final Network network = Network.newNetwork();

            final MongoDBInstance mongoDB = MongoDBInstance.createStartedWithUniqueName(network, Lifecycle.CLASS, mongodbVersion);
            final MailServerContainer emailServerInstance = withMailServerEnabled ? MailServerContainer.createStarted(network) : null;

            final var builder = SearchServerInstanceProvider.getBuilderFor(searchVersion).orElseThrow(() -> new UnsupportedOperationException("Search version " + searchVersion + " not supported."));
            SearchServerInstance searchServer = builder
                    .network(network)
                    .mongoDbUri(mongoDB.internalUri())
                    .passwordSecret(PASSWORD_SECRET)
                    .rootPasswordSha2(ROOT_PASSWORD_SHA_2)
                    .featureFlags(enabledFeatureFlags)
                    .build();

            return new Services(network, searchServer, mongoDB, emailServerInstance);
        }

        private Services(Network network, SearchServerInstance searchServer, MongoDBInstance mongoDBInstance, @Nullable MailServerContainer mailServerContainer) {
            this.network = network;
            this.searchServerInstance = searchServer;
            this.mongoDBInstance = mongoDBInstance;
            this.mailServerContainer = mailServerContainer;
        }

        private static String buildLookupKey(SearchVersion searchVersion, MongodbServer mongodbVersion, boolean withMailServerEnabled, List<String> enabledFeatureFlags) {
            return "%s-%s-%s-%s".formatted(searchVersion.toString(), mongodbVersion.toString(), withMailServerEnabled ? "mail" : "nomail", enabledFeatureFlags);
        }

        public MongoDBInstance getMongoDBInstance() {
            return this.mongoDBInstance;
        }

        public Network getNetwork() {
            return network;
        }

        public SearchServerInstance getSearchServerInstance() {
            return searchServerInstance;
        }

        public MailServerContainer getMailServerContainer() {
            return mailServerContainer;
        }

        @Override
        public void close() throws Exception {
            Stream<AutoCloseable> closeables = Stream.of(mailServerContainer, mongoDBInstance, searchServerInstance, network);
            closeables.filter(Objects::nonNull).forEach(autoCloseable -> {
                try {
                    autoCloseable.close();
                } catch (Exception ignore) {
                }
            });
        }

        public void cleanUp() {
            mongoDBInstance.dropDatabase();
            searchServerInstance.cleanUp();
        }
    }
}
