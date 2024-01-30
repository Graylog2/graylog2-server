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
package org.graylog.datanode.initializers;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jakarta.rs.base.JsonMappingExceptionMapper;
import com.fasterxml.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;
import com.google.common.collect.ImmutableMap;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.DynamicFeature;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.ExceptionMapper;
import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.model.Resource;
import org.graylog.datanode.Configuration;
import org.graylog.datanode.configuration.variants.KeystoreInformation;
import org.graylog.datanode.configuration.variants.OpensearchSecurityConfiguration;
import org.graylog.datanode.management.OpensearchConfigurationChangeEvent;
import org.graylog.datanode.process.OpensearchConfiguration;
import org.graylog.datanode.rest.config.SecuredNodeAnnotationFilter;
import org.graylog.security.certutil.CertConstants;
import org.graylog2.bootstrap.preflight.web.BasicAuthFilter;
import org.graylog2.configuration.TLSProtocolsConfiguration;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.shared.rest.exceptionmappers.JacksonPropertyExceptionMapper;
import org.graylog2.shared.rest.exceptionmappers.JsonProcessingExceptionMapper;
import org.graylog2.shared.security.tls.KeyStoreUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

public class JerseyService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(JerseyService.class);
    private static final String RESOURCE_PACKAGE_WEB = "org.graylog2.web.resources";

    private final Configuration configuration;
    private final Set<Class<?>> systemRestResources;

    private final Set<Class<? extends DynamicFeature>> dynamicFeatures;
    private final Set<Class<? extends ExceptionMapper>> exceptionMappers;
    private final ObjectMapper objectMapper;
    private final MetricRegistry metricRegistry;
    private final TLSProtocolsConfiguration tlsConfiguration;

    private HttpServer apiHttpServer = null;

    @Inject
    public JerseyService(final Configuration configuration,
                         Set<Class<? extends DynamicFeature>> dynamicFeatures,
                         Set<Class<? extends ExceptionMapper>> exceptionMappers,
                         @Named(Graylog2Module.SYSTEM_REST_RESOURCES) final Set<Class<?>> systemRestResources,
                         ObjectMapper objectMapper,
                         MetricRegistry metricRegistry,
                         TLSProtocolsConfiguration tlsConfiguration, EventBus eventBus) {
        this.configuration = requireNonNull(configuration, "configuration");
        this.dynamicFeatures = requireNonNull(dynamicFeatures, "dynamicFeatures");
        this.exceptionMappers = requireNonNull(exceptionMappers, "exceptionMappers");
        this.systemRestResources = systemRestResources;
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.metricRegistry = requireNonNull(metricRegistry, "metricRegistry");
        this.tlsConfiguration = requireNonNull(tlsConfiguration);
        eventBus.register(this);
    }

    @Subscribe
    public synchronized void handleOpensearchConfigurationChange(OpensearchConfigurationChangeEvent event) throws Exception {
        if (apiHttpServer == null) {
            // this is the very first start of the jersey service
            LOG.info("Starting Data node REST API");
        } else {
            // jersey service has been running for some time, now we received new configuration. We'll reboot the service
            LOG.info("Server configuration changed, restarting Data node REST API to apply security changes");
        }
        shutDown();
        doStartup(extractSslConfiguration(event.config()));
    }

    private SSLEngineConfigurator extractSslConfiguration(OpensearchConfiguration config) throws GeneralSecurityException, IOException {
        final OpensearchSecurityConfiguration securityConfiguration = config.opensearchSecurityConfiguration();
        if (securityConfiguration != null && securityConfiguration.securityEnabled()) {
            return buildSslEngineConfigurator(securityConfiguration.getHttpCertificate());
        } else {
            return null;
        }

    }

    @Override
    protected void startUp() {
        // do nothing, the actual startup will be triggered at the moment opensearch configuration is available
    }

    private void doStartup(SSLEngineConfigurator sslEngineConfigurator) throws Exception {
        // we need to work around the change introduced in https://github.com/GrizzlyNIO/grizzly-mirror/commit/ba9beb2d137e708e00caf7c22603532f753ec850
        // because the PooledMemoryManager which is default now uses 10% of the heap no matter what
        System.setProperty("org.glassfish.grizzly.DEFAULT_MEMORY_MANAGER", "org.glassfish.grizzly.memory.HeapMemoryManager");
        startUpApi(sslEngineConfigurator);
    }

    @Override
    protected void shutDown() {
        shutdownHttpServer(apiHttpServer, HostAndPort.fromParts(configuration.getBindAddress(), configuration.getDatanodeHttpPort()));
    }

    private void shutdownHttpServer(HttpServer httpServer, HostAndPort bindAddress) {
        if (httpServer != null && httpServer.isStarted()) {
            LOG.info("Shutting down HTTP listener at <{}>", bindAddress);
            httpServer.shutdownNow();
        }
    }

    private void startUpApi(SSLEngineConfigurator sslEngineConfigurator) throws Exception {
        final String contextPath = configuration.getHttpPublishUri().getPath();
        final URI listenUri = new URI(
                configuration.getUriScheme(),
                null,
                configuration.getBindAddress(),
                configuration.getDatanodeHttpPort(),
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

        LOG.info("Started REST API at <{}:{}>", configuration.getBindAddress(), configuration.getDatanodeHttpPort());
    }

    private ResourceConfig buildResourceConfig(final Set<Resource> additionalResources) {
        final ResourceConfig rc = new ResourceConfig()
                .property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true)
                .property(ServerProperties.WADL_FEATURE_DISABLE, true)
                .property(ServerProperties.MEDIA_TYPE_MAPPINGS, mediaTypeMappings())
                .registerClasses(
                        JacksonXmlBindJsonProvider.class,
                        JsonProcessingExceptionMapper.class,
                        JsonMappingExceptionMapper.class,
                        JacksonPropertyExceptionMapper.class)
                // Replacing this with a lambda leads to missing subtypes - https://github.com/Graylog2/graylog2-server/pull/10617#discussion_r630236360
                .register(new ContextResolver<ObjectMapper>() {
                    @Override
                    public ObjectMapper getContext(Class<?> type) {
                        return objectMapper;
                    }
                })
                .register(MultiPartFeature.class)
                .registerClasses(systemRestResources)
                .registerResources(additionalResources);

        exceptionMappers.forEach(rc::registerClasses);
        dynamicFeatures.forEach(rc::registerClasses);

        return rc;
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
        final boolean isSecuredInstance = sslEngineConfigurator != null;
        final ResourceConfig resourceConfig = buildResourceConfig(additionalResources);

        if (isSecuredInstance) {
            resourceConfig.register(createAuthFilter(configuration));
        }
        resourceConfig.register(new SecuredNodeAnnotationFilter(configuration.isInsecureStartup()));

        final HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(
                listenUri,
                resourceConfig,
                isSecuredInstance,
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

    @NotNull
    private ContainerRequestFilter createAuthFilter(Configuration configuration) {
        final ContainerRequestFilter basicAuthFilter = new BasicAuthFilter(configuration.getRootUsername(), configuration.getRootPasswordSha2(), "Datanode");
        final AuthTokenValidator tokenVerifier = new JwtTokenValidator(configuration.getPasswordSecret());
        return new DatanodeAuthFilter(basicAuthFilter, tokenVerifier);
    }

    private SSLEngineConfigurator buildSslEngineConfigurator(KeystoreInformation keystoreInformation)
            throws GeneralSecurityException, IOException {
        if (keystoreInformation == null || !Files.isRegularFile(keystoreInformation.location()) || !Files.isReadable(keystoreInformation.location())) {
            throw new IllegalArgumentException("Unreadable to read private key");
        }


        final SSLContextConfigurator sslContextConfigurator = new SSLContextConfigurator();
        final char[] password = firstNonNull(keystoreInformation.passwordAsString(), "").toCharArray();

        final KeyStore keyStore = readKeystore(keystoreInformation);

        sslContextConfigurator.setKeyStorePass(password);
        sslContextConfigurator.setKeyStoreBytes(KeyStoreUtils.getBytes(keyStore, password));

        final SSLContext sslContext = sslContextConfigurator.createSSLContext(true);
        final SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(sslContext, false, false, false);
        sslEngineConfigurator.setEnabledProtocols(tlsConfiguration.getEnabledTlsProtocols().toArray(new String[0]));
        return sslEngineConfigurator;
    }

    private static KeyStore readKeystore(KeystoreInformation keystoreInformation) {
        try (var in = Files.newInputStream(keystoreInformation.location())) {
            KeyStore caKeystore = KeyStore.getInstance(CertConstants.PKCS12);
            caKeystore.load(in, keystoreInformation.password());
            return caKeystore;
        } catch (IOException | GeneralSecurityException ex) {
            throw new RuntimeException("Could not read keystore: " + ex.getMessage(), ex);
        }
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
                name(JerseyService.class, executorName));
    }
}
