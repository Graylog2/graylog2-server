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
import org.glassfish.jersey.server.model.Resource;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.rest.PluginRestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Singleton
public class RestApiService extends AbstractJerseyService {
    private static final Logger LOG = LoggerFactory.getLogger(RestApiService.class);
    public static final String PLUGIN_PREFIX = "/plugins";

    private final BaseConfiguration configuration;
    private final Map<String, Set<PluginRestResource>> pluginRestResources;
    private final String[] restControllerPackages;

    @Inject
    private RestApiService(final BaseConfiguration configuration,
                           final MetricRegistry metricRegistry,
                           final Set<Class<? extends DynamicFeature>> dynamicFeatures,
                           final Set<Class<? extends ContainerResponseFilter>> containerResponseFilters,
                           final Set<Class<? extends ExceptionMapper>> exceptionMappers,
                           @Named("additionalJerseyComponents") final Set<Class> additionalComponents,
                           final Map<String, Set<PluginRestResource>> pluginRestResources,
                           @Named("RestControllerPackages") final String[] restControllerPackages,
                           final ObjectMapper objectMapper) {
        super(dynamicFeatures, containerResponseFilters, exceptionMappers, additionalComponents, objectMapper, metricRegistry);
        this.configuration = configuration;
        this.pluginRestResources = pluginRestResources;
        this.restControllerPackages = restControllerPackages;
    }

    @Override
    protected void startUp() throws Exception {
        httpServer = setUp("rest",
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
                prefixPluginResources(PLUGIN_PREFIX, pluginRestResources),
                restControllerPackages);

        httpServer.start();

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
        if (httpServer != null && httpServer.isStarted()) {
            LOG.info("Shutting down REST API at <{}>", configuration.getRestListenUri());
            httpServer.shutdownNow();
        }
    }
}
