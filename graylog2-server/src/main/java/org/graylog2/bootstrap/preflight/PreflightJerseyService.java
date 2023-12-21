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
package org.graylog2.bootstrap.preflight;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.base.JsonMappingExceptionMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import org.apache.commons.codec.digest.DigestUtils;
import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.model.Resource;
import org.graylog2.Configuration;
import org.graylog2.bootstrap.preflight.web.BasicAuthFilter;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.shared.rest.exceptionmappers.AnyExceptionClassMapper;
import org.graylog2.shared.rest.exceptionmappers.JacksonPropertyExceptionMapper;
import org.graylog2.shared.rest.exceptionmappers.JsonProcessingExceptionMapper;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import java.net.URI;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

public class PreflightJerseyService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(PreflightJerseyService.class);

    private final HttpConfiguration configuration;
    private final Set<Class<?>> systemRestResources;

    private final ObjectMapper objectMapper;
    private final MetricRegistry metricRegistry;
    private final Configuration localConfiguration;
    private final PreflightConfigService preflightConfigService;

    private HttpServer apiHttpServer = null;

    @Inject
    public PreflightJerseyService(final HttpConfiguration httpConfiguration,
                                  final Configuration localConfiguration,
                                  @PreflightRestResourcesBinding final Set<Class<?>> systemRestResources,
                                  ObjectMapper objectMapper,
                                  MetricRegistry metricRegistry,
                                  PreflightConfigService preflightConfigService) {
        this.configuration = requireNonNull(httpConfiguration, "configuration");
        this.localConfiguration = requireNonNull(localConfiguration, "localConfiguration");
        this.preflightConfigService = requireNonNull(preflightConfigService, "preflightConfigService");
        this.systemRestResources = systemRestResources;
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.metricRegistry = requireNonNull(metricRegistry, "metricRegistry");
    }

    @Override
    protected void startUp() throws Exception {
        // we need to work around the change introduced in https://github.com/GrizzlyNIO/grizzly-mirror/commit/ba9beb2d137e708e00caf7c22603532f753ec850
        // because the PooledMemoryManager which is default now uses 10% of the heap no matter what
        System.setProperty("org.glassfish.grizzly.DEFAULT_MEMORY_MANAGER", "org.glassfish.grizzly.memory.HeapMemoryManager");
        startUpApi();
    }

    @Override
    protected void shutDown() throws Exception {
        shutdownHttpServer(apiHttpServer, configuration.getHttpBindAddress());
    }

    private void shutdownHttpServer(HttpServer httpServer, HostAndPort bindAddress) {
        if (httpServer != null && httpServer.isStarted()) {
            LOG.info("Shutting down HTTP listener at <{}>", bindAddress);
            httpServer.shutdownNow();
        }
    }

    private void startUpApi() throws Exception {
        final SSLEngineConfigurator sslEngineConfigurator = null;

        final HostAndPort bindAddress = configuration.getHttpBindAddress();
        final String contextPath = configuration.getHttpPublishUri().getPath();
        final URI listenUri = new URI(
                configuration.getUriScheme(),
                null,
                bindAddress.getHost(),
                bindAddress.getPort(),
                isNullOrEmpty(contextPath) ? "/" : contextPath,
                null,
                null
        );

        apiHttpServer = setUp(
                listenUri,
                sslEngineConfigurator,
                configuration.getHttpThreadPoolSize(),
                configuration.getHttpSelectorRunnersCount(),
                configuration.getHttpMaxHeaderSize(),
                configuration.isHttpEnableGzip(),
                Set.of());

        apiHttpServer.start();

        final var username = localConfiguration.getRootUsername();
        final var preflightPassword = preflightConfigService.getPreflightPassword();
        logBanner(bindAddress, username, preflightPassword);
    }

    private void logBanner(HostAndPort bindAddress, String username, String preflightPassword) {
        var banner = """

                                                                             ---
                                                                             ---
                                                                             ---
                    ########  ###   ######### ##########   ####         #### ---         .----               ----
                  ###############   ###################### #####       ####  ---      ------------       .----------- --
                 #####     ######   #####              #### ####      ####   ---     ---        ---     ---        -----
                ####         ####   ####       ############  ####     ####   ---    --           ---   ---           ---
                ###           ###   ####     ##############   ####   ####    ---   ---            --   --             --
                ####         ####   ####    ####       ####    #### ####     ---   ---            --   --            .--
                #####       #####   ####    ####       ####     #######      ---    ---          ---   ---           ---
                 ################   ####     ##############     ######-       --     ----      ----      ---       -----
                   ##############   ####      #############      #####        -----   -----------         ----------  --
                             ####                                ####                                                ---
                #####       ####                                ####                                     -          .--
                  #############                                ####                                     -----     ----
                     ######                                   ####                                          -------

                ========================================================================================================

                It seems you are starting Graylog for the first time. To set up a fresh install, a setup interface has
                been started. You must log in to it to perform the initial configuration and continue.

                Initial configuration is accessible at %s, with username '%s' and password '%s'.

                ========================================================================================================
                """.formatted(bindAddress, username, preflightPassword);

        LOG.info(banner);
    }

    private ResourceConfig buildResourceConfig(final Set<Resource> additionalResources) {
        return new ResourceConfig()
                .property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true)
                .property(ServerProperties.WADL_FEATURE_DISABLE, true)
                .property(ServerProperties.MEDIA_TYPE_MAPPINGS, mediaTypeMappings())
                .registerClasses(
                        JacksonXmlBindJsonProvider.class,
                        JsonProcessingExceptionMapper.class,
                        JsonMappingExceptionMapper.class,
                        JacksonPropertyExceptionMapper.class,
                        AnyExceptionClassMapper.class
                )
                // Replacing this with a lambda leads to missing subtypes - https://github.com/Graylog2/graylog2-server/pull/10617#discussion_r630236360
                .register(new ContextResolver<ObjectMapper>() {
                    @Override
                    public ObjectMapper getContext(Class<?> type) {
                        return objectMapper;
                    }
                })
                .register(MultiPartFeature.class)
                .registerClasses(systemRestResources)
                .registerResources(additionalResources)
                .register(createBasicAuthFilter(localConfiguration, preflightConfigService));
    }

    @NotNull
    private BasicAuthFilter createBasicAuthFilter(Configuration localConfiguration, PreflightConfigService preflightConfigService) {
        final String username = localConfiguration.getRootUsername();
        final String preflightPassword = preflightConfigService.getPreflightPassword();
        return new BasicAuthFilter(username, DigestUtils.sha256Hex(preflightPassword), "preflight-config");
    }

    private Map<String, MediaType> mediaTypeMappings() {
        return ImmutableMap.of(
                "json", MediaType.APPLICATION_JSON_TYPE,
                "ndjson", MoreMediaTypes.APPLICATION_NDJSON_TYPE,
                "csv", MoreMediaTypes.TEXT_CSV_TYPE,
                "log", MoreMediaTypes.TEXT_PLAIN_TYPE
        );
    }

    private HttpServer setUp(URI listenUri,
                             SSLEngineConfigurator sslEngineConfigurator,
                             int threadPoolSize,
                             int selectorRunnersCount,
                             int maxHeaderSize,
                             boolean enableGzip,
                             Set<Resource> additionalResources) {
        final ResourceConfig resourceConfig = buildResourceConfig(additionalResources);
        final HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(
                listenUri,
                resourceConfig,
                sslEngineConfigurator != null,
                sslEngineConfigurator,
                false);

        final NetworkListener listener = httpServer.getListener("grizzly");
        listener.setMaxHttpHeaderSize(maxHeaderSize);

        final ExecutorService workerThreadPoolExecutor = instrumentedExecutor(
                "http-worker-executor",
                "http-worker-%d",
                threadPoolSize);
        listener.getTransport().setWorkerThreadPool(workerThreadPoolExecutor);

        // The Grizzly default value is equal to `Runtime.getRuntime().availableProcessors()` which doesn't make
        // sense for Graylog because we are not mainly a web server.
        // See "Selector runners count" at https://grizzly.java.net/bestpractices.html for details.
        listener.getTransport().setSelectorRunnersCount(selectorRunnersCount);


        if (enableGzip) {
            final CompressionConfig compressionConfig = listener.getCompressionConfig();
            compressionConfig.setCompressionMode(CompressionConfig.CompressionMode.ON);
            compressionConfig.setCompressionMinSize(512);
        }

        return httpServer;
    }

    private ExecutorService instrumentedExecutor(final String executorName,
                                                 final String threadNameFormat,
                                                 int poolSize) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder()
                .setNameFormat(threadNameFormat)
                .setDaemon(true)
                .build();

        return new InstrumentedExecutorService(
                Executors.newFixedThreadPool(poolSize, threadFactory),
                metricRegistry,
                name(PreflightJerseyService.class, executorName));
    }
}
