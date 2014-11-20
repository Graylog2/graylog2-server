/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.shared.initializers;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.InstrumentedThreadFactory;
import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.internal.util.$Nullable;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.glassfish.jersey.server.model.Resource;
import org.graylog2.jersey.container.netty.NettyContainer;
import org.graylog2.jersey.container.netty.SecurityContextFactory;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.rest.AnyExceptionClassMapper;
import org.graylog2.plugin.rest.JacksonPropertyExceptionMapper;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.rest.WebApplicationExceptionMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.graylog2.shared.rest.CORSFilter;
import org.graylog2.shared.rest.PrintModelProcessor;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.execution.ExecutionHandler;
import org.jboss.netty.handler.execution.OrderedMemoryAwareThreadPoolExecutor;
import org.jboss.netty.handler.ssl.SslContext;
import org.jboss.netty.handler.ssl.SslHandler;
import org.jboss.netty.handler.ssl.util.SelfSignedCertificate;
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.net.ssl.SSLException;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ExceptionMapper;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.URI;
import java.security.cert.CertificateException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;

@Singleton
public class RestApiService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(RestApiService.class);

    private final BaseConfiguration configuration;
    private final MetricRegistry metricRegistry;
    private final SecurityContextFactory securityContextFactory;
    private final Set<Class<? extends DynamicFeature>> dynamicFeatures;
    private final Set<Class<? extends ContainerResponseFilter>> containerResponseFilters;
    private final Set<Class<? extends ExceptionMapper>> exceptionMappers;
    private final Map<String, Set<PluginRestResource>> pluginRestResources;

    private final ServerBootstrap bootstrap;

    @Inject
    public RestApiService(BaseConfiguration configuration,
                          MetricRegistry metricRegistry,
                          @$Nullable SecurityContextFactory securityContextFactory,
                          Set<Class<? extends DynamicFeature>> dynamicFeatures,
                          Set<Class<? extends ContainerResponseFilter>> containerResponseFilters,
                          Set<Class<? extends ExceptionMapper>> exceptionMappers,
                          Map<String, Set<PluginRestResource>> pluginRestResources) {
        this(configuration, metricRegistry, securityContextFactory, dynamicFeatures, containerResponseFilters,
                exceptionMappers, pluginRestResources,
                instrumentedExecutor("restapi-boss-%d", metricRegistry),
                instrumentedExecutor("restapi-worker-%d", metricRegistry));
    }

    private RestApiService(final BaseConfiguration configuration,
                           final MetricRegistry metricRegistry,
                           final SecurityContextFactory securityContextFactory,
                           final Set<Class<? extends DynamicFeature>> dynamicFeatures,
                           final Set<Class<? extends ContainerResponseFilter>> containerResponseFilters,
                           final Set<Class<? extends ExceptionMapper>> exceptionMappers,
                           final Map<String, Set<PluginRestResource>> pluginRestResources,
                           final ExecutorService bossExecutor,
                           final ExecutorService workerExecutor) {
        this(configuration, metricRegistry, securityContextFactory, dynamicFeatures, containerResponseFilters,
                exceptionMappers, pluginRestResources, buildServerBootStrap(bossExecutor, workerExecutor));
    }

    private RestApiService(final BaseConfiguration configuration,
                           final MetricRegistry metricRegistry,
                           final SecurityContextFactory securityContextFactory,
                           final Set<Class<? extends DynamicFeature>> dynamicFeatures,
                           final Set<Class<? extends ContainerResponseFilter>> containerResponseFilters,
                           final Set<Class<? extends ExceptionMapper>> exceptionMappers,
                           final Map<String, Set<PluginRestResource>> pluginRestResources,
                           final ServerBootstrap bootstrap) {
        this.configuration = configuration;
        this.metricRegistry = metricRegistry;
        this.securityContextFactory = securityContextFactory;
        this.dynamicFeatures = dynamicFeatures;
        this.containerResponseFilters = containerResponseFilters;
        this.exceptionMappers = exceptionMappers;
        this.pluginRestResources = pluginRestResources;
        this.bootstrap = bootstrap;
    }

    private static ExecutorService instrumentedExecutor(final String nameFormat, final MetricRegistry metricRegistry) {
        return new InstrumentedExecutorService(
                Executors.newCachedThreadPool(threadFactory(nameFormat, metricRegistry)), metricRegistry);
    }

    private static ThreadFactory threadFactory(final String nameFormat, final MetricRegistry metricRegistry) {
        return new InstrumentedThreadFactory(
                new ThreadFactoryBuilder().setNameFormat(nameFormat).build(), metricRegistry);
    }

    private static ServerBootstrap buildServerBootStrap(final ExecutorService bossExecutor, final ExecutorService workerExecutor) {
        return new ServerBootstrap(new NioServerSocketChannelFactory(bossExecutor, workerExecutor));
    }

    @Override
    protected void startUp() throws Exception {
        final NettyContainer jerseyHandler = ContainerFactory.createContainer(NettyContainer.class,
                buildResourceConfig(
                        configuration.isRestEnableGzip(),
                        configuration.isRestEnableCors(),
                        prefixPluginResources("/plugins", pluginRestResources)));

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

            LOG.info("rest_tls_cert_file or rest_tls_key_file is empty. Using self-signed certificates instead.");
            LOG.debug("rest_tls_cert_file = {}", tlsCertFile);
            LOG.debug("rest_tls_key_file = {}", tlsKeyFile);
        } else {
            tlsCertFile = configuration.getRestTlsCertFile();
            tlsKeyFile = configuration.getRestTlsKeyFile();
        }

        // TODO Magic numbers
        final ExecutorService executor = new InstrumentedExecutorService(new OrderedMemoryAwareThreadPoolExecutor(configuration.getRestThreadPoolSize(), 1048576, 1048576), metricRegistry);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                final ChannelPipeline pipeline = Channels.pipeline();

                if (configuration.isRestEnableTls()) {
                    pipeline.addLast("tls", buildSslHandler());
                }

                pipeline.addLast("decoder", new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize));
                pipeline.addLast("encoder", new HttpResponseEncoder());
                pipeline.addLast("chunks", new ChunkedWriteHandler());
                pipeline.addLast("executor", new ExecutionHandler(executor));
                pipeline.addLast("jerseyHandler", jerseyHandler);

                return pipeline;
            }

            private SslHandler buildSslHandler() throws CertificateException, SSLException {
                final SslContext sslCtx = SslContext.newServerContext(
                        tlsCertFile, tlsKeyFile, emptyToNull(configuration.getRestTlsKeyPassword()));

                return sslCtx.newHandler();
            }
        });
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);

        bootstrap.bind(new InetSocketAddress(
                configuration.getRestListenUri().getHost(),
                configuration.getRestListenUri().getPort()
        ));

        LOG.info("Started REST API at <{}>", configuration.getRestListenUri());
    }

    @SuppressWarnings("unchecked")
    private ResourceConfig buildResourceConfig(final boolean enableGzip,
                                               final boolean enableCors,
                                               final Set<Resource> additionalResources) {
        final URI listenUri;
        if (isNullOrEmpty(configuration.getRestListenUri().getPath())) {
            listenUri = UriBuilder.fromUri(configuration.getRestListenUri()).path("/").build();
        } else {
            listenUri = configuration.getRestListenUri();
        }

        ResourceConfig rc = new ResourceConfig()
                .property(NettyContainer.PROPERTY_BASE_URI, listenUri)
                .registerClasses(
                        JacksonPropertyExceptionMapper.class,
                        AnyExceptionClassMapper.class,
                        WebApplicationExceptionMapper.class)
                .register(ObjectMapperProvider.class)
                .register(JacksonFeature.class)
                .registerFinder(new PackageNamesScanner(new String[]{
                        "org.graylog2.rest.resources",
                        "org.graylog2.radio.rest.resources",
                        "org.graylog2.shared.rest.resources"
                }, true))
                .registerResources(additionalResources);

        for (Class<? extends ExceptionMapper> exceptionMapper : exceptionMappers) {
            rc.registerClasses(exceptionMapper);
        }

        for (Class<? extends DynamicFeature> dynamicFeatureClass : dynamicFeatures) {
            rc.registerClasses(dynamicFeatureClass);
        }

        for (Class<? extends ContainerResponseFilter> responseFilter : containerResponseFilters) {
            rc.registerClasses(responseFilter);
        }

        if (enableGzip) {
            EncodingFilter.enableFor(rc, GZipEncoder.class);
        }

        if (enableCors) {
            LOG.info("Enabling CORS for REST API");
            rc.register(CORSFilter.class);
        }

        if (LOG.isDebugEnabled()) {
            rc.register(PrintModelProcessor.class);
        }

        return rc;
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
