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
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import org.glassfish.jersey.server.model.Resource;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.web.resources.AppConfigResource;
import org.graylog2.web.resources.WebInterfaceAssetsResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.Path;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Singleton
public class RestApiService extends AbstractJerseyService {
    private static final Logger LOG = LoggerFactory.getLogger(RestApiService.class);
    public static final String PLUGIN_PREFIX = "/plugins";

    private final BaseConfiguration configuration;
    private final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources;
    private final String[] restControllerPackages;
    private final WebInterfaceAssetsResource webInterfaceAssetsResource;
    private final AppConfigResource appConfigResource;

    @Inject
    private RestApiService(final BaseConfiguration configuration,
                           final MetricRegistry metricRegistry,
                           final Set<Class<? extends DynamicFeature>> dynamicFeatures,
                           final Set<Class<? extends ContainerResponseFilter>> containerResponseFilters,
                           final Set<Class<? extends ExceptionMapper>> exceptionMappers,
                           @Named("additionalJerseyComponents") final Set<Class> additionalComponents,
                           final Map<String, Set<Class<? extends PluginRestResource>>> pluginRestResources,
                           @Named("RestControllerPackages") final String[] restControllerPackages,
                           final ObjectMapper objectMapper,
                           WebInterfaceAssetsResource webInterfaceAssetsResource,
                           AppConfigResource appConfigResource) {
        super(dynamicFeatures, containerResponseFilters, exceptionMappers, additionalComponents, objectMapper, metricRegistry);
        this.configuration = configuration;
        this.pluginRestResources = pluginRestResources;
        this.restControllerPackages = restControllerPackages;
        this.webInterfaceAssetsResource = webInterfaceAssetsResource;
        this.appConfigResource = appConfigResource;
    }

    @Override
    protected void startUp() throws Exception {
        ImmutableSet.Builder<Resource> additionalResourcesBuilder = ImmutableSet
            .<Resource>builder()
            .addAll(prefixPluginResources(PLUGIN_PREFIX, pluginRestResources));

        if (configuration.isWebEnable() && configuration.isRestAndWebOnSamePort()) {
            additionalResourcesBuilder = additionalResourcesBuilder
                .addAll(prefixResources(configuration.getWebPrefix(), ImmutableSet.of(webInterfaceAssetsResource.getClass(), appConfigResource.getClass())));
        }

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
                additionalResourcesBuilder.build(),
                restControllerPackages);

        httpServer.start();

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
        final String pathPrefix = prefix.endsWith("/") ? prefix.substring(0, prefix.length()-1) : prefix;

        return resources
            .stream()
            .map(resource -> {
                final Path pathAnnotation = Resource.getPath(resource);
                final String resourcePathSuffix = Strings.nullToEmpty(pathAnnotation.value());
                final String resourcePath = resourcePathSuffix.startsWith("/") ? pathPrefix + resourcePathSuffix : pathPrefix + "/" + resourcePathSuffix;

                return Resource
                    .builder(resource)
                    .path(resourcePath)
                    .build();
            })
            .collect(Collectors.toSet());
    }

    @Override
    protected void shutDown() throws Exception {
        if (httpServer != null && httpServer.isStarted()) {
            LOG.info("Shutting down REST API at <{}>", configuration.getRestListenUri());
            httpServer.shutdownNow();
        }
    }
}
