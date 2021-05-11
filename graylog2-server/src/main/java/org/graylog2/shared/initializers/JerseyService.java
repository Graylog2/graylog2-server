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
package org.graylog2.shared.initializers;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.net.HostAndPort;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.glassfish.grizzly.http.CompressionConfig;
import org.glassfish.grizzly.http.server.ErrorPageGenerator;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.model.Resource;
import org.graylog.security.UserContextBinder;
import org.graylog2.Configuration;
import org.graylog2.audit.PluginAuditEventTypes;
import org.graylog2.audit.jersey.AuditEventModelProcessor;
import org.graylog2.configuration.HttpConfiguration;
import org.graylog2.jersey.PrefixAddingModelProcessor;
import org.graylog2.plugin.inject.Graylog2Module;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.MoreMediaTypes;
import org.graylog2.rest.filter.WebAppNotFoundResponseFilter;
import org.graylog2.shared.rest.CORSFilter;
import org.graylog2.shared.rest.NodeIdResponseFilter;
import org.graylog2.shared.rest.NotAuthorizedResponseFilter;
import org.graylog2.shared.rest.PrintModelProcessor;
import org.graylog2.shared.rest.RequestIdFilter;
import org.graylog2.shared.rest.RestAccessLogFilter;
import org.graylog2.shared.rest.VerboseCsrfProtectionFilter;
import org.graylog2.shared.rest.XHRFilter;
import org.graylog2.shared.rest.exceptionmappers.AnyExceptionClassMapper;
import org.graylog2.shared.rest.exceptionmappers.BadRequestExceptionMapper;
import org.graylog2.shared.rest.exceptionmappers.JacksonPropertyExceptionMapper;
import org.graylog2.shared.rest.exceptionmappers.JsonMappingExceptionMapper;
import org.graylog2.shared.rest.exceptionmappers.JsonProcessingExceptionMapper;
import org.graylog2.shared.rest.exceptionmappers.MissingStreamPermissionExceptionMapper;
import org.graylog2.shared.rest.exceptionmappers.WebApplicationExceptionMapper;
import org.graylog2.shared.security.ShiroRequestHeadersBinder;
import org.graylog2.shared.security.ShiroSecurityContextFilter;
import org.graylog2.shared.security.tls.KeyStoreUtils;
import org.graylog2.shared.security.tls.PemKeyStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.net.ssl.SSLContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

public class JerseyService extends AbstractIdleService {
    public static final String PLUGIN_PREFIX = "/plugins";
    private static final Logger LOG = LoggerFactory.getLogger(JerseyService.class);
    private static final String RESOURCE_PACKAGE_WEB = "org.graylog2.web.resources";

    private final HttpConfiguration configuration;
    private final Configuration graylogConfiguration;
    private final Set<Class<?>> systemRestResources;
    private final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources;

    private final Set<Class<? extends DynamicFeature>> dynamicFeatures;
    private final Set<Class<? extends ContainerResponseFilter>> containerResponseFilters;
    private final Set<Class<? extends ExceptionMapper>> exceptionMappers;
    private final Set<Class> additionalComponents;
    private final Set<PluginAuditEventTypes> pluginAuditEventTypes;
    private final ObjectMapper objectMapper;
    private final MetricRegistry metricRegistry;
    private final ErrorPageGenerator errorPageGenerator;

    private HttpServer apiHttpServer = null;

    @Inject
    public JerseyService(final HttpConfiguration configuration,
                         Configuration graylogConfiguration, Set<Class<? extends DynamicFeature>> dynamicFeatures,
                         Set<Class<? extends ContainerResponseFilter>> containerResponseFilters,
                         Set<Class<? extends ExceptionMapper>> exceptionMappers,
                         @Named("additionalJerseyComponents") final Set<Class> additionalComponents,
                         @Named(Graylog2Module.SYSTEM_REST_RESOURCES) final Set<Class<?>> systemRestResources,
                         final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources,
                         Set<PluginAuditEventTypes> pluginAuditEventTypes,
                         ObjectMapper objectMapper,
                         MetricRegistry metricRegistry,
                         ErrorPageGenerator errorPageGenerator) {
        this.configuration = requireNonNull(configuration, "configuration");
        this.graylogConfiguration = graylogConfiguration;
        this.dynamicFeatures = requireNonNull(dynamicFeatures, "dynamicFeatures");
        this.containerResponseFilters = requireNonNull(containerResponseFilters, "containerResponseFilters");
        this.exceptionMappers = requireNonNull(exceptionMappers, "exceptionMappers");
        this.additionalComponents = requireNonNull(additionalComponents, "additionalComponents");
        this.systemRestResources = systemRestResources;
        this.pluginRestResources = requireNonNull(pluginRestResources, "pluginResources");
        this.pluginAuditEventTypes = requireNonNull(pluginAuditEventTypes, "pluginAuditEventTypes");
        this.objectMapper = requireNonNull(objectMapper, "objectMapper");
        this.metricRegistry = requireNonNull(metricRegistry, "metricRegistry");
        this.errorPageGenerator = requireNonNull(errorPageGenerator, "errorPageGenerator");
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
        final Set<Resource> pluginResources = prefixPluginResources(PLUGIN_PREFIX, pluginRestResources);

        final SSLEngineConfigurator sslEngineConfigurator = configuration.isHttpEnableTls() ?
                buildSslEngineConfigurator(
                        configuration.getHttpTlsCertFile(),
                        configuration.getHttpTlsKeyFile(),
                        configuration.getHttpTlsKeyPassword()) : null;

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
                configuration.isHttpEnableCors(),
                pluginResources);

        apiHttpServer.start();

        LOG.info("Started REST API at <{}>", configuration.getHttpBindAddress());
    }

    private Set<Resource> prefixPluginResources(String pluginPrefix, Map<String, Set<Class<? extends PluginRestResource>>> pluginResourceMap) {
        return pluginResourceMap.entrySet().stream()
                .map(entry -> prefixResources(pluginPrefix + "/" + entry.getKey(), entry.getValue()))
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
    }

    private <T> Set<Resource> prefixResources(String prefix, Set<Class<? extends T>> resources) {
        final String pathPrefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length() - 1) : prefix;

        return resources
                .stream()
                .map(resource -> {
                    final javax.ws.rs.Path pathAnnotation = Resource.getPath(resource);
                    final String resourcePathSuffix = Strings.nullToEmpty(pathAnnotation.value());
                    final String resourcePath = resourcePathSuffix.startsWith("/") ? pathPrefix + resourcePathSuffix : pathPrefix + "/" + resourcePathSuffix;

                    return Resource
                            .builder(resource)
                            .path(resourcePath)
                            .build();
                })
                .collect(Collectors.toSet());
    }


    private ResourceConfig buildResourceConfig(final boolean enableCors,
                                               final Set<Resource> additionalResources) {
        final Map<String, String> packagePrefixes = ImmutableMap.of(
                RESOURCE_PACKAGE_WEB, HttpConfiguration.PATH_WEB,
                "", HttpConfiguration.PATH_API
        );

        final ResourceConfig rc = new ResourceConfig()
                .property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true)
                .property(ServerProperties.WADL_FEATURE_DISABLE, true)
                .property(ServerProperties.MEDIA_TYPE_MAPPINGS, mediaTypeMappings())
                .register(new PrefixAddingModelProcessor(packagePrefixes, graylogConfiguration))
                .register(new AuditEventModelProcessor(pluginAuditEventTypes))
                .registerClasses(
                        ShiroSecurityContextFilter.class,
                        ShiroRequestHeadersBinder.class,
                        VerboseCsrfProtectionFilter.class,
                        JacksonJaxbJsonProvider.class,
                        JsonProcessingExceptionMapper.class,
                        JsonMappingExceptionMapper.class,
                        JacksonPropertyExceptionMapper.class,
                        AnyExceptionClassMapper.class,
                        MissingStreamPermissionExceptionMapper.class,
                        WebApplicationExceptionMapper.class,
                        BadRequestExceptionMapper.class,
                        RestAccessLogFilter.class,
                        NodeIdResponseFilter.class,
                        RequestIdFilter.class,
                        XHRFilter.class,
                        NotAuthorizedResponseFilter.class,
                        WebAppNotFoundResponseFilter.class)
                .register((ContextResolver<ObjectMapper>) type -> objectMapper)
                .register(new UserContextBinder())
                .register(MultiPartFeature.class)
                .registerClasses(systemRestResources)
                .registerResources(additionalResources);

        exceptionMappers.forEach(rc::registerClasses);
        dynamicFeatures.forEach(rc::registerClasses);
        containerResponseFilters.forEach(rc::registerClasses);
        additionalComponents.forEach(rc::registerClasses);

        if (enableCors) {
            LOG.info("Enabling CORS for HTTP endpoint");
            rc.registerClasses(CORSFilter.class);
        }

        if (LOG.isDebugEnabled()) {
            rc.registerClasses(PrintModelProcessor.class);
        }

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
                             boolean enableCors,
                             Set<Resource> additionalResources) {
        final ResourceConfig resourceConfig = buildResourceConfig(enableCors, additionalResources);
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

        listener.setDefaultErrorPageGenerator(errorPageGenerator);

        if (enableGzip) {
            final CompressionConfig compressionConfig = listener.getCompressionConfig();
            compressionConfig.setCompressionMode(CompressionConfig.CompressionMode.ON);
            compressionConfig.setCompressionMinSize(512);
        }

        return httpServer;
    }

    private SSLEngineConfigurator buildSslEngineConfigurator(Path certFile, Path keyFile, String keyPassword)
            throws GeneralSecurityException, IOException {
        if (keyFile == null || !Files.isRegularFile(keyFile) || !Files.isReadable(keyFile)) {
            throw new InvalidKeyException("Unreadable or missing private key: " + keyFile);
        }

        if (certFile == null || !Files.isRegularFile(certFile) || !Files.isReadable(certFile)) {
            throw new CertificateException("Unreadable or missing X.509 certificate: " + certFile);
        }

        final SSLContextConfigurator sslContextConfigurator = new SSLContextConfigurator();
        final char[] password = firstNonNull(keyPassword, "").toCharArray();
        final KeyStore keyStore = PemKeyStore.buildKeyStore(certFile, keyFile, password);
        sslContextConfigurator.setKeyStorePass(password);
        sslContextConfigurator.setKeyStoreBytes(KeyStoreUtils.getBytes(keyStore, password));

        final SSLContext sslContext = sslContextConfigurator.createSSLContext(true);
        final SSLEngineConfigurator sslEngineConfigurator = new SSLEngineConfigurator(sslContext, false, false, false);
        if (!graylogConfiguration.getEnabledTlsProtocols().isEmpty()) {
            sslEngineConfigurator.setEnabledProtocols(graylogConfiguration.getEnabledTlsProtocols().toArray(new String[0]));
        }
        return sslEngineConfigurator;
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
