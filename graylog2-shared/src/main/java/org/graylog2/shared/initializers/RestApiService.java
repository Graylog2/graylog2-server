/**
 * The MIT License
 * Copyright (c) 2012 TORCH GmbH
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.graylog2.shared.initializers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.google.common.util.concurrent.AbstractIdleService;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.internal.util.$Nullable;
import org.glassfish.jersey.message.GZipEncoder;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.filter.EncodingFilter;
import org.glassfish.jersey.server.internal.scanning.PackageNamesScanner;
import org.glassfish.jersey.server.model.Resource;
import org.graylog2.jersey.container.netty.NettyContainer;
import org.graylog2.jersey.container.netty.SecurityContextFactory;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.rest.WebApplicationExceptionMapper;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.rest.AnyExceptionClassMapper;
import org.graylog2.plugin.rest.JacksonPropertyExceptionMapper;
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
import org.jboss.netty.handler.stream.ChunkedWriteHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.ext.ExceptionMapper;
import java.net.InetSocketAddress;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@Singleton
public class RestApiService extends AbstractIdleService {
    private static final Logger LOG = LoggerFactory.getLogger(RestApiService.class);
    private final BaseConfiguration configuration;
    private final SecurityContextFactory securityContextFactory;
    private final Set<Class<? extends DynamicFeature>> dynamicFeatures;
    private final Set<Class<? extends ContainerResponseFilter>> containerResponseFilters;
    private final Set<Class<? extends ExceptionMapper>> exceptionMappers;
    private final Map<String, Set<PluginRestResource>> pluginRestResources;
    private final ObjectMapper objectMapper;
    private ServerBootstrap bootstrap;

    @Inject
    public RestApiService(BaseConfiguration configuration,
                          @$Nullable SecurityContextFactory securityContextFactory,
                          Set<Class<? extends DynamicFeature>> dynamicFeatures,
                          Set<Class<? extends ContainerResponseFilter>> containerResponseFilters,
                          Set<Class<? extends ExceptionMapper>> exceptionMappers,
                          Map<String, Set<PluginRestResource>> pluginRestResources,
                          ObjectMapper objectMapper) {
        this.configuration = configuration;
        this.securityContextFactory = securityContextFactory;
        this.dynamicFeatures = dynamicFeatures;
        this.containerResponseFilters = containerResponseFilters;
        this.exceptionMappers = exceptionMappers;
        this.pluginRestResources = pluginRestResources;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void startUp() throws Exception {
        final ExecutorService bossExecutor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("restapi-boss-%d")
                        .build());

        final ExecutorService workerExecutor = Executors.newCachedThreadPool(
                new ThreadFactoryBuilder()
                        .setNameFormat("restapi-worker-%d")
                        .build());

        bootstrap = new ServerBootstrap(new NioServerSocketChannelFactory(
                bossExecutor,
                workerExecutor
        ));

        ResourceConfig rc = new ResourceConfig()
                .property(NettyContainer.PROPERTY_BASE_URI, configuration.getRestListenUri())
                .registerClasses(JacksonPropertyExceptionMapper.class,
                        AnyExceptionClassMapper.class, WebApplicationExceptionMapper.class);

        for (Class<? extends ExceptionMapper> exceptionMapper : exceptionMappers)
            rc.registerClasses(exceptionMapper);

        for (Class<? extends DynamicFeature> dynamicFeatureClass : dynamicFeatures)
            rc.registerClasses(dynamicFeatureClass);

        for (Class<? extends ContainerResponseFilter> responseFilter : containerResponseFilters)
            rc.registerClasses(responseFilter);

        rc
            .register(new JacksonJsonProvider(objectMapper))
            .registerFinder(new PackageNamesScanner(new String[]{"org.graylog2.rest.resources",
                    "org.graylog2.radio.rest.resources", "org.graylog2.shared.rest.resources"}, true));

        if (configuration.isRestEnableGzip())
            EncodingFilter.enableFor(rc, GZipEncoder.class);

        if (configuration.isRestEnableCors()) {
            LOG.info("Enabling CORS for REST API");
            rc.register(CORSFilter.class);
        }

        rc.registerResources(prefixPluginResources("/plugins", pluginRestResources));

        if(LOG.isDebugEnabled())
            rc.register(PrintModelProcessor.class);

        final NettyContainer jerseyHandler = ContainerFactory.createContainer(NettyContainer.class, rc);
        if (securityContextFactory != null) {
            LOG.info("Adding security context factory: <{}>", securityContextFactory);
            jerseyHandler.setSecurityContextFactory(securityContextFactory);
        } else {
            LOG.info("Not adding security context factory.");
        }

        final int maxInitialLineLength = configuration.getRestMaxInitialLineLength();
        final int maxHeaderSize = configuration.getRestMaxHeaderSize();
        final int maxChunkSize = configuration.getRestMaxChunkSize();

        final ThreadPoolExecutor executor = new OrderedMemoryAwareThreadPoolExecutor(16, 1048576, 1048576);

        bootstrap.setPipelineFactory(new ChannelPipelineFactory() {
            @Override
            public ChannelPipeline getPipeline() throws Exception {
                ChannelPipeline pipeline = Channels.pipeline();
                pipeline.addLast("decoder", new HttpRequestDecoder(maxInitialLineLength, maxHeaderSize, maxChunkSize));
                pipeline.addLast("encoder", new HttpResponseEncoder());
                pipeline.addLast("chunks", new ChunkedWriteHandler());
                pipeline.addLast("executor", new ExecutionHandler(executor));
                pipeline.addLast("jerseyHandler", jerseyHandler);
                return pipeline;
            }
        }) ;
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
