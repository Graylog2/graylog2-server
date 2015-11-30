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

import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.model.Resource;
import org.graylog2.jersey.container.netty.NettyContainer;
import org.graylog2.jersey.container.netty.SecurityContextFactory;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.rest.PluginRestResource;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.handler.ssl.util.SelfSignedCertificate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.ext.ExceptionMapper;
import java.io.File;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;

@Singleton
public class RestApiService extends AbstractJerseyService {
    private static final Logger LOG = LoggerFactory.getLogger(RestApiService.class);

    private final BaseConfiguration configuration;
    private final MetricRegistry metricRegistry;
    private final SecurityContextFactory securityContextFactory;
    private final Map<String, Set<PluginRestResource>> pluginRestResources;

    private final ServerBootstrap bootstrap;
    private final String[] restControllerPackages;

    @Inject
    public RestApiService(BaseConfiguration configuration,
                          MetricRegistry metricRegistry,
                          @Nullable SecurityContextFactory securityContextFactory,
                          Set<Class<? extends DynamicFeature>> dynamicFeatures,
                          Set<Class<? extends ContainerResponseFilter>> containerResponseFilters,
                          Set<Class<? extends ExceptionMapper>> exceptionMappers,
                          @Named("additionalJerseyComponents") Set<Class> additionalComponents,
                          Map<String, Set<PluginRestResource>> pluginRestResources,
                          @Named("RestControllerPackages") String[] restControllerPackages,
                          Provider<ObjectMapper> objectMapperProvider) {
        this(configuration, metricRegistry, securityContextFactory, dynamicFeatures, containerResponseFilters,
                exceptionMappers, additionalComponents, pluginRestResources,
                instrumentedExecutor("boss-executor-service", "restapi-boss-%d", metricRegistry),
                instrumentedExecutor("worker-executor-service", "restapi-worker-%d", metricRegistry),
                restControllerPackages, objectMapperProvider);
    }

    private RestApiService(final BaseConfiguration configuration,
                           final MetricRegistry metricRegistry,
                           final SecurityContextFactory securityContextFactory,
                           final Set<Class<? extends DynamicFeature>> dynamicFeatures,
                           final Set<Class<? extends ContainerResponseFilter>> containerResponseFilters,
                           final Set<Class<? extends ExceptionMapper>> exceptionMappers,
                           final Set<Class> additionalComponents,
                           final Map<String, Set<PluginRestResource>> pluginRestResources,
                           final ExecutorService bossExecutor,
                           final ExecutorService workerExecutor,
                           final String[] restControllerPackages,
                           Provider<ObjectMapper> objectMapperProvider) {
        this(configuration, metricRegistry, securityContextFactory, dynamicFeatures,
                containerResponseFilters, exceptionMappers, additionalComponents, pluginRestResources,
                buildServerBootStrap(bossExecutor, workerExecutor, configuration.getRestWorkerThreadsMaxPoolSize()),
                restControllerPackages, objectMapperProvider);
    }

    private RestApiService(final BaseConfiguration configuration,
                           final MetricRegistry metricRegistry,
                           final SecurityContextFactory securityContextFactory,
                           final Set<Class<? extends DynamicFeature>> dynamicFeatures,
                           final Set<Class<? extends ContainerResponseFilter>> containerResponseFilters,
                           final Set<Class<? extends ExceptionMapper>> exceptionMappers,
                           final Set<Class> additionalComponents,
                           final Map<String, Set<PluginRestResource>> pluginRestResources,
                           final ServerBootstrap bootstrap,
                           final String[] restControllerPackages,
                           Provider<ObjectMapper> objectMapperProvider) {
        super(dynamicFeatures, containerResponseFilters, exceptionMappers, additionalComponents, objectMapperProvider);
        this.configuration = configuration;
        this.metricRegistry = metricRegistry;
        this.securityContextFactory = securityContextFactory;
        this.pluginRestResources = pluginRestResources;
        this.bootstrap = bootstrap;
        this.restControllerPackages = restControllerPackages;
    }

    @Override
    protected void startUp() throws Exception {
        final NettyContainer jerseyHandler = ContainerFactory.createContainer(NettyContainer.class,
                buildResourceConfig(
                        configuration.isRestEnableGzip(),
                        configuration.isRestEnableCors(),
                        prefixPluginResources("/plugins", pluginRestResources),
                        configuration.getRestListenUri(),
                        restControllerPackages)
        );

        if (securityContextFactory != null) {
            LOG.info("Adding security context factory: <{}>", securityContextFactory);
            jerseyHandler.setSecurityContextFactory(securityContextFactory);
        } else {
            LOG.info("Not adding security context factory.");
        }

        final int maxInitialLineLength = configuration.getRestMaxInitialLineLength();
        final int maxHeaderSize = configuration.getRestMaxHeaderSize();
        final int maxChunkSize = configuration.getRestMaxChunkSize();

        final File tlsCertFile;
        final File tlsKeyFile;
        if (configuration.isRestEnableTls() && (configuration.getRestTlsCertFile() == null || configuration.getRestTlsKeyFile() == null)) {
            final SelfSignedCertificate ssc = new SelfSignedCertificate(configuration.getRestListenUri().getHost());
            tlsCertFile = ssc.certificate();
            tlsKeyFile = ssc.privateKey();

            LOG.warn("rest_tls_cert_file or rest_tls_key_file is empty. Using self-signed certificates instead.");
            LOG.debug("rest_tls_cert_file = {}", tlsCertFile);
            LOG.debug("rest_tls_key_file = {}", tlsKeyFile);
        } else {
            tlsCertFile = configuration.getRestTlsCertFile();
            tlsKeyFile = configuration.getRestTlsKeyFile();
        }

        final Executor executor = buildPoolExecutor("restapi-execution-handler", configuration.getRestThreadPoolSize(), metricRegistry);

        bootstrap.setPipelineFactory(buildPipelineFactory(
                configuration.isRestEnableTls(),
                maxInitialLineLength,
                maxHeaderSize,
                maxChunkSize,
                executor,
                jerseyHandler,
                tlsCertFile,
                tlsKeyFile,
                configuration.getRestTlsKeyPassword()
        ));
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        bootstrap.bind(new InetSocketAddress(
                configuration.getRestListenUri().getHost(),
                configuration.getRestListenUri().getPort()
        ));

        LOG.info("Started REST API at <{}>", configuration.getRestListenUri());
    }

    private Set<Resource> prefixPluginResources(String pluginPrefix, Map<String, Set<PluginRestResource>> pluginResourceMap) {
        final Set<Resource> result = new HashSet<>();
        for (Map.Entry<String, Set<PluginRestResource>> entry : pluginResourceMap.entrySet()) {
            for (PluginRestResource pluginRestResource : entry.getValue()) {
                StringBuilder resourcePath = new StringBuilder(pluginPrefix).append("/").append(entry.getKey());
                final Path pathAnnotation = Resource.getPath(pluginRestResource.getClass());
                final String path = (pathAnnotation.value() == null ? "" : pathAnnotation.value());
                if (!path.startsWith("/"))
                    resourcePath.append("/");

                final Resource.Builder resourceBuilder = Resource.builder(pluginRestResource.getClass()).path(resourcePath.append(path).toString());
                final Resource resource = resourceBuilder.build();
                result.add(resource);
            }
        }
        return result;
    }

    @Override
    protected void shutDown() throws Exception {
        LOG.info("Shutting down REST API at <{}>", configuration.getRestListenUri());
        bootstrap.releaseExternalResources();
        bootstrap.shutdown();
    }
}
