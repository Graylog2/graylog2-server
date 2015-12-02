package org.graylog2.shared.initializers;

import com.codahale.metrics.InstrumentedExecutorService;
import com.codahale.metrics.MetricRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.glassfish.jersey.server.model.Resource;
import org.graylog2.jersey.container.netty.NettyContainer;
import org.graylog2.shared.rest.CORSFilter;
import org.graylog2.shared.rest.NodeIdResponseFilter;
import org.graylog2.shared.rest.PrintModelProcessor;
import org.graylog2.shared.rest.exceptionmappers.AnyExceptionClassMapper;
import org.graylog2.shared.rest.exceptionmappers.BadRequestExceptionMapper;
import org.graylog2.shared.rest.exceptionmappers.JacksonPropertyExceptionMapper;
import org.graylog2.shared.rest.exceptionmappers.JsonProcessingExceptionMapper;
import org.graylog2.shared.rest.exceptionmappers.WebApplicationExceptionMapper;
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
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Provider;
import javax.net.ssl.SSLException;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ExceptionMapper;
import java.io.File;
import java.net.URI;
import java.security.cert.CertificateException;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;
import static com.google.common.base.Strings.emptyToNull;
import static com.google.common.base.Strings.isNullOrEmpty;

public abstract class AbstractJerseyService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(AbstractJerseyService.class);

    private final Set<Class<? extends DynamicFeature>> dynamicFeatures;
    private final Set<Class<? extends ContainerResponseFilter>> containerResponseFilters;
    private final Set<Class<? extends ExceptionMapper>> exceptionMappers;
    private final Set<Class> additionalComponents;
    private final Provider<ObjectMapper> objectMapperProvider;

    public AbstractJerseyService(Set<Class<? extends DynamicFeature>> dynamicFeatures,
                                 Set<Class<? extends ContainerResponseFilter>> containerResponseFilters,
                                 Set<Class<? extends ExceptionMapper>> exceptionMappers,
                                 Set<Class> additionalComponents,
                                 Provider<ObjectMapper> objectMapperProvider) {
        this.dynamicFeatures = dynamicFeatures;
        this.containerResponseFilters = containerResponseFilters;
        this.exceptionMappers = exceptionMappers;
        this.additionalComponents = additionalComponents;
        this.objectMapperProvider = objectMapperProvider;
    }

    protected static ExecutorService instrumentedExecutor(final String executorName, final String threadNameFormat, final MetricRegistry metricRegistry) {
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(threadNameFormat).build();
        return new InstrumentedExecutorService(
                Executors.newCachedThreadPool(threadFactory),
                metricRegistry,
                name(RestApiService.class, executorName));
    }

    protected static ServerBootstrap buildServerBootStrap(final ExecutorService bossExecutor,
                                                        final ExecutorService workerExecutor, final int workerCount) {
        return new ServerBootstrap(new NioServerSocketChannelFactory(bossExecutor, workerExecutor, workerCount));
    }

    @SuppressWarnings("unchecked")
    protected ResourceConfig buildResourceConfig(final boolean enableGzip,
                                               final boolean enableCors,
                                               final Set<Resource> additionalResources,
                                               final URI configuredListenUri,
                                               final String[] controllerPackages) {
        final URI listenUri;
        if (isNullOrEmpty(configuredListenUri.getPath())) {
            listenUri = UriBuilder.fromUri(configuredListenUri).path("/").build();
        } else {
            listenUri = configuredListenUri;
        }

        final ObjectMapper objectMapper = objectMapperProvider.get();
        ResourceConfig rc = new ResourceConfig()
                .property(NettyContainer.PROPERTY_BASE_URI, listenUri)
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
                .register(NodeIdResponseFilter.class);

        for (Class<? extends ExceptionMapper> exceptionMapper : exceptionMappers) {
            rc.registerClasses(exceptionMapper);
        }

        for (Class<? extends DynamicFeature> dynamicFeatureClass : dynamicFeatures) {
            rc.registerClasses(dynamicFeatureClass);
        }

        for (Class<? extends ContainerResponseFilter> responseFilter : containerResponseFilters) {
            rc.registerClasses(responseFilter);
        }

        for (Class additionalComponent : additionalComponents)
            rc.registerClasses(additionalComponent);

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

    protected ChannelPipelineFactory buildPipelineFactory(final boolean enableTls,
                                   final int maxInitialLineLength,
                                   final int maxHeaderSize,
                                   final int maxChunkSize,
                                   final Executor executor,
                                   final NettyContainer jerseyHandler,
                                   final File tlsCertFile,
                                   final File tlsKeyFile,
                                   final String tlsKeyPassword) {
        return new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                final ChannelPipeline pipeline = Channels.pipeline();

                if (enableTls) {
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
                        tlsCertFile, tlsKeyFile, emptyToNull(tlsKeyPassword));

                return sslCtx.newHandler();
            }
        };
    }

    protected Executor buildPoolExecutor(String namePrefix, int threadPoolSize, MetricRegistry metricRegistry) {
        // TODO Magic numbers
        final ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat(namePrefix + "-%d").build();
        return new InstrumentedExecutorService(
                new OrderedMemoryAwareThreadPoolExecutor(
                        threadPoolSize,
                        1048576,
                        1048576,
                        30, TimeUnit.SECONDS,
                        threadFactory),
                metricRegistry,
                name(this.getClass(), namePrefix + "-executor-service"));

    }
}
