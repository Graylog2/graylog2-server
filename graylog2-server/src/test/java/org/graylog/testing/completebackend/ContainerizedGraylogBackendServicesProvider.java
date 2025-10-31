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

import com.google.common.base.Stopwatch;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.graylog.testing.elasticsearch.SearchServerInstance;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog.testing.mongodb.MongoDBVersion;
import org.graylog2.plugin.Tools;
import org.graylog2.storage.SearchVersion;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.Network;

import javax.annotation.Nullable;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;

import static java.util.Objects.requireNonNull;
import static org.graylog.testing.completebackend.ContainerizedGraylogBackend.PASSWORD_SECRET;
import static org.graylog.testing.completebackend.ContainerizedGraylogBackend.ROOT_PASSWORD_SHA_2;

/**
 * Provides all services (searchDB, mongoDB, ...) that a {@link ContainerizedGraylogBackend} depends on.
 * Bundling these services in one class allows us to re-use them in multiple test runs
 * without the need to restart them every time.
 * The {@link ContainerizedGraylogBackend} using these services must run a {@link Services#cleanUp()}}
 * after it shuts down.
 */
public class ContainerizedGraylogBackendServicesProvider {

    private static final Logger LOG = LoggerFactory.getLogger(ContainerizedGraylogBackendServicesProvider.class);

    private static final ConcurrentMap<String, Services> SERVICES_CACHE = new ConcurrentHashMap<>();
    private final Lifecycle lifecycle;

    @Deprecated(forRemoval = true) // use constructor with lifecycle parameter
    public ContainerizedGraylogBackendServicesProvider() {
        this(Lifecycle.CLASS);
    }

    public ContainerizedGraylogBackendServicesProvider(Lifecycle lifecycle) {
        this.lifecycle = lifecycle;
    }

    public Services getServices(SearchVersion searchVersion,
                                MongoDBVersion mongodbVersion,
                                List<String> enabledFeatureFlags,
                                Map<String, String> env,
                                PluginJarsProvider datanodePluginJarsProvider) {
        var lookupKey = Services.buildLookupKey(lifecycle, searchVersion, mongodbVersion, enabledFeatureFlags, env, datanodePluginJarsProvider);
        return SERVICES_CACHE.computeIfAbsent(lookupKey, (k) -> {
            LOG.info("No cached services found for key \"{}\", creating new ones.", k);
            //noinspection resource
            return Services.create(
                    searchVersion,
                    mongodbVersion,
                    enabledFeatureFlags,
                    env,
                    datanodePluginJarsProvider,
                    () -> SERVICES_CACHE.remove(k)
            );
        });
    }

    public static void clearServicesCache() {
        SERVICES_CACHE.values().forEach(s -> {
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

        private final WebhookServerContainer webhookServerInstance;
        private final Runnable cacheRemovalCallback;


        private static Services create(SearchVersion searchVersion,
                                       MongoDBVersion mongodbVersion,
                                       List<String> enabledFeatureFlags,
                                       Map<String, String> env,
                                       PluginJarsProvider datanodePluginJarsProvider,
                                       Runnable closeCallback) {
            try (var executorService = Executors.newFixedThreadPool(3, new ThreadFactoryBuilder()
                    .setNameFormat("container-startup-thread-%d")
                    .setDaemon(true)
                    .setUncaughtExceptionHandler(new Tools.LogUncaughtExceptionHandler(LOG))
                    .build())) {
                final Network network = Network.newNetwork();

                final Future<MongoDBInstance> mongodbFuture = executorService.submit(
                        withStopwatch(() -> MongoDBInstance.createUncachedStarted(network, mongodbVersion), "MongoDB"));
                final Future<MailServerContainer> mailServerContainerFuture = executorService.submit(
                        withStopwatch(() -> MailServerContainer.createStarted(network), "Mailserver"));
                final Future<WebhookServerContainer> webhookServerContainerFuture = executorService.submit(
                        withStopwatch(() -> WebhookServerContainer.createStarted(network), "WebhookTester"));
                final MongoDBInstance mongoDB = mongodbFuture.get();
                final MailServerContainer emailServerInstance = mailServerContainerFuture.get();
                final WebhookServerContainer webhookServerInstance = webhookServerContainerFuture.get();

                executorService.shutdownNow();

                final Stopwatch searchServerSw = Stopwatch.createStarted();
                final var builder = SearchServerInstanceProvider.getBuilderFor(searchVersion)
                        .orElseThrow(() -> new UnsupportedOperationException(
                                "Search version " + searchVersion + " not supported."));
                LOG.debug("Starting search server: {}", searchVersion);
                final SearchServerInstance searchServer = builder
                        .cachedInstance(false) // This service layer caches OpenSearch instances itself
                        .network(network)
                        .mongoDbUri(MongoDBInstance.internalUri())
                        .passwordSecret(PASSWORD_SECRET)
                        .rootPasswordSha2(ROOT_PASSWORD_SHA_2)
                        .featureFlags(enabledFeatureFlags)
                        .env(env)
                        .datanodePluginJarsProvider(datanodePluginJarsProvider)
                        .build();
                LOG.debug("Startup of the search server {} took {} (instance: {})", searchVersion,
                        searchServerSw.elapsed(), searchServer.instanceId());
                return new Services(network, searchServer, mongoDB, emailServerInstance, webhookServerInstance, closeCallback);
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }

        }

        private static <T> Callable<T> withStopwatch(Callable<T> delegate, String containerName) {
            return () -> {
                Stopwatch stopwatch = Stopwatch.createStarted();
                final T result = delegate.call();
                if (result != null) {
                    LOG.debug("Startup of {} took {}", containerName, stopwatch.elapsed());
                }
                return result;
            };
        }

        private Services(Network network,
                         SearchServerInstance searchServer,
                         MongoDBInstance mongoDBInstance,
                         @Nullable MailServerContainer mailServerContainer,
                         @Nullable WebhookServerContainer webhookServerInstance,
                         Runnable cacheRemovalCallback) {
            this.network = network;
            this.searchServerInstance = searchServer;
            this.mongoDBInstance = mongoDBInstance;
            this.mailServerContainer = mailServerContainer;
            this.webhookServerInstance = webhookServerInstance;
            this.cacheRemovalCallback = requireNonNull(cacheRemovalCallback, "cacheRemovalCallback can't be null");
        }

        private static String buildLookupKey(Lifecycle lifecycle,
                                             SearchVersion searchVersion,
                                             MongoDBVersion mongodbVersion,
                                             List<String> enabledFeatureFlags,
                                             Map<String, String> env,
                                             PluginJarsProvider datanodePluginJarsProvider) {
            List<String> parts = new LinkedList<>();
            parts.add(lifecycle.name());
            parts.add(searchVersion.toString());
            parts.add("MongoDB:" + mongodbVersion.version());
            parts.addAll(enabledFeatureFlags);
            parts.addAll(env.entrySet().stream().map(e -> e.getKey() + ":" + e.getValue()).toList());
            parts.add("DataNode-plugins:" + datanodePluginJarsProvider.getUniqueId());
            return String.join("-", parts);
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
            // Call this first, so the services are removed from the cache before we actually close them.
            cacheRemovalCallback.run();

            Stream.of(mailServerContainer, mongoDBInstance, searchServerInstance, network)
                    .filter(Objects::nonNull)
                    .forEach(autoCloseable -> {
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

        public WebhookServerInstance getWebhookServerContainer() {
            return webhookServerInstance;
        }
    }
}
