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
package org.graylog2.shared.initializers;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.ssl.SSLContextConfigurator;
import org.glassfish.grizzly.ssl.SSLEngineConfigurator;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.glassfish.jersey.server.model.Resource;
import org.graylog2.Configuration;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.CORSFilter;
import org.graylog2.shared.rest.NodeIdResponseFilter;
import org.graylog2.shared.rest.NotAuthorizedResponseFilter;
import org.graylog2.shared.rest.PrintModelProcessor;
import org.graylog2.shared.rest.RestAccessLogFilter;
import org.graylog2.shared.rest.XHRFilter;
import org.graylog2.shared.rest.exceptionmappers.AnyExceptionClassMapper;
import org.graylog2.shared.rest.exceptionmappers.BadRequestExceptionMapper;
import org.graylog2.shared.rest.exceptionmappers.JacksonPropertyExceptionMapper;
import org.graylog2.shared.rest.exceptionmappers.JsonProcessingExceptionMapper;
import org.graylog2.shared.rest.exceptionmappers.WebApplicationExceptionMapper;
import org.graylog2.shared.security.tls.KeyStoreUtils;
import org.graylog2.shared.security.tls.PemKeyStore;
import org.graylog2.web.resources.AppConfigResource;
import org.graylog2.web.resources.WebInterfaceAssetsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
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
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.Collectors;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.MoreObjects.firstNonNull;

public class JerseyService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(JerseyService.class);
    public static final String PLUGIN_PREFIX = "/plugins";

    private final Configuration configuration;
    private final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources;
    private final String[] restControllerPackages;

    private final Set<Class<? extends DynamicFeature>> dynamicFeatures;
    private final Set<Class<? extends ContainerResponseFilter>> containerResponseFilters;
    private final Set<Class<? extends ExceptionMapper>> exceptionMappers;
    private final Set<Class> additionalComponents;
    private final ObjectMapper objectMapper;
    private final MetricRegistry metricRegistry;

    private HttpServer apiHttpServer = null;
    private HttpServer webHttpServer = null;

    @Inject
    public JerseyService(final Configuration configuration,
                         Set<Class<? extends DynamicFeature>> dynamicFeatures,
                         Set<Class<? extends ContainerResponseFilter>> containerResponseFilters,
                         Set<Class<? extends ExceptionMapper>> exceptionMappers,
                         @Named("additionalJerseyComponents") final Set<Class> additionalComponents,
                         final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources,
                         @Named("RestControllerPackages") final String[] restControllerPackages,
                         ObjectMapper objectMapper,
                         MetricRegistry metricRegistry) {
        this.configuration = configuration;
        this.dynamicFeatures = dynamicFeatures;
        this.containerResponseFilters = containerResponseFilters;
        this.exceptionMappers = exceptionMappers;
        this.additionalComponents = additionalComponents;
        this.pluginRestResources = pluginRestResources;
        this.restControllerPackages = restControllerPackages;
        this.objectMapper = objectMapper;
        this.metricRegistry = metricRegistry;
    }

    @Override
    protected void startUp() throws Exception {
        startUpApi();
        if (configuration.isWebEnable() && !configuration.isRestAndWebOnSamePort()) {
            startUpWeb();
        }
    }

    private void startUpWeb() throws Exception {
        final String[] resources = new String[]{"org.graylog2.web.resources"};

        webHttpServer = setUp("web",
                configuration.getWebListenUri(),
                configuration.isWebEnableTls(),
                configuration.getWebTlsCertFile(),
                configuration.getWebTlsKeyFile(),
                configuration.getWebTlsKeyPassword(),
                configuration.getWebThreadPoolSize(),
                configuration.getWebMaxInitialLineLength(),
                configuration.getWebMaxHeaderSize(),
                configuration.isWebEnableGzip(),
                configuration.isWebEnableCors(),
                Collections.emptySet(),
                resources);

        webHttpServer.start();

        LOG.info("Started Web Interface at <{}>", configuration.getWebListenUri());
    }

    @Override
    protected void shutDown() throws Exception {
        if (apiHttpServer != null && apiHttpServer.isStarted()) {
            LOG.info("Shutting down REST API at <{}>", configuration.getRestListenUri());
            apiHttpServer.shutdownNow();
        }

        if (webHttpServer != null && webHttpServer.isStarted()) {
            LOG.info("Shutting down Web Interface at <{}>", configuration.getWebListenUri());
            webHttpServer.shutdownNow();
        }
    }

    private void startUpApi() throws Exception {
        final ImmutableSet.Builder<Resource> additionalResourcesBuilder = ImmutableSet.<Resource>builder()
                .addAll(prefixPluginResources(PLUGIN_PREFIX, pluginRestResources));

        if (configuration.isWebEnable() && configuration.isRestAndWebOnSamePort()) {
            additionalResourcesBuilder
                    .addAll(prefixResources(configuration.getWebPrefix(), ImmutableSet.of(WebInterfaceAssetsResource.class, AppConfigResource.class)));
        }

        apiHttpServer = setUp("rest",
                configuration.getRestListenUri(),
                configuration.isRestEnableTls(),
                configuration.getRestTlsCertFile(),
                configuration.getRestTlsKeyFile(),
                configuration.getRestTlsKeyPassword(),
                configuration.getRestThreadPoolSize(),
                configuration.getRestMaxInitialLineLength(),
                configuration.getRestMaxHeaderSize(),
                configuration.isRestEnableGzip(),
                configuration.isRestEnableCors(),
                additionalResourcesBuilder.build(),
                restControllerPackages);

        apiHttpServer.start();

        LOG.info("Started REST API at <{}>", configuration.getRestListenUri());

        if (configuration.isWebEnable() && configuration.isRestAndWebOnSamePort()) {
            LOG.info("Started Web Interface at <{}>", configuration.getWebListenUri());
        }
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


    @SuppressWarnings("unchecked")
    private ResourceConfig buildResourceConfig(final boolean enableGzip,
                                               final boolean enableCors,
                                               final Set<Resource> additionalResources,
                                               final String[] controllerPackages) {
        final ResourceConfig rc = new ResourceConfig()
                .property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true)
                .property(ServerProperties.WADL_FEATURE_DISABLE, true)
                .registerClasses(
                        JacksonJaxbJsonProvider.class,
                        JsonProcessingExceptionMapper.class,
                        JacksonPropertyExceptionMapper.class,
                        AnyExceptionClassMapper.class,
                        WebApplicationExceptionMapper.class,
                        BadRequestExceptionMapper.class)
                .register(new ContextResolver<ObjectMapper>() {
                    @Override
                    public ObjectMapper getContext(Class<?> type) {
                        return objectMapper;
                    }
                })
                .registerFinder(new PackageNamesScanner(controllerPackages, true))
                .registerResources(additionalResources)
                .register(RestAccessLogFilter.class)
                .register(NodeIdResponseFilter.class)
                .register(XHRFilter.class)
                .register(NotAuthorizedResponseFilter.class);

        exceptionMappers.forEach(rc::registerClasses);
        dynamicFeatures.forEach(rc::registerClasses);
        containerResponseFilters.forEach(rc::registerClasses);
        additionalComponents.forEach(rc::registerClasses);

        if (enableGzip) {
            EncodingFilter.enableFor(rc, GZipEncoder.class);
        }

        if (enableCors) {
            LOG.info("Enabling CORS for HTTP endpoint");
            rc.register(CORSFilter.class);
        }

        if (LOG.isDebugEnabled()) {
            rc.register(PrintModelProcessor.class);
        }

        return rc;
    }

    private HttpServer setUp(String namePrefix, URI listenUri,
                               boolean enableTls, Path tlsCertFile, Path tlsKeyFile, String tlsKeyPassword,
                               int threadPoolSize, int maxInitialLineLength, int maxHeaderSize,
                               boolean enableGzip, boolean enableCors,
                               Set<Resource> additionalResources, String[] controllerPackages)
            throws GeneralSecurityException, IOException {
        final ResourceConfig resourceConfig = buildResourceConfig(
                enableGzip,
                enableCors,
                additionalResources,
                controllerPackages
        );

        final SSLEngineConfigurator sslEngineConfigurator = enableTls ?
                buildSslEngineConfigurator(tlsCertFile, tlsKeyFile, tlsKeyPassword) : null;

        final HttpServer httpServer = GrizzlyHttpServerFactory.createHttpServer(listenUri, resourceConfig, enableTls, sslEngineConfigurator);

        final NetworkListener listener = httpServer.getListener("grizzly");
        listener.setMaxHttpHeaderSize(maxInitialLineLength);
        listener.setMaxRequestHeaders(maxHeaderSize);

        final ExecutorService workerThreadPoolExecutor = instrumentedExecutor(
                namePrefix + "-worker-executor",
                namePrefix + "-worker-%d",
                threadPoolSize);
        listener.getTransport().setWorkerThreadPool(workerThreadPoolExecutor);

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

        final SSLContextConfigurator sslContext = new SSLContextConfigurator();
        final char[] password = firstNonNull(keyPassword, "").toCharArray();
        final KeyStore keyStore = PemKeyStore.buildKeyStore(certFile, keyFile, password);
        sslContext.setKeyStorePass(password);
        sslContext.setKeyStoreBytes(KeyStoreUtils.getBytes(keyStore, password));

        if (!sslContext.validateConfiguration(true)) {
            throw new IllegalStateException("Couldn't initialize SSL context for HTTP server");
        }

        return new SSLEngineConfigurator(sslContext.createSSLContext(), false, false, false);
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
