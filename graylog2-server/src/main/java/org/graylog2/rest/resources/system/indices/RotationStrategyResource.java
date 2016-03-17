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
package org.graylog2.rest.resources.system.indices;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.indexer.management.IndexManagementConfig;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.rest.models.system.indices.RotationStrategies;
import org.graylog2.rest.models.system.indices.RotationStrategyDescription;
import org.graylog2.rest.models.system.indices.RotationStrategySummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.hibernate.validator.constraints.NotEmpty;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Api(value = "System/Indices/Rotation", description = "Index rotation strategy settings")
@Path("/system/indices/rotation")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class RotationStrategyResource extends RestResource {
    private final Map<String, Provider<RotationStrategy>> rotationStrategies;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public RotationStrategyResource(Map<String, Provider<RotationStrategy>> rotationStrategies,
                                    ClusterConfigService clusterConfigService) {
        this.rotationStrategies = requireNonNull(rotationStrategies);
        this.clusterConfigService = requireNonNull(clusterConfigService);
    }

    @GET
    @Path("config")
    @Timed
    @ApiOperation(value = "Configuration of the current rotation strategy",
            notes = "This resource returns the configuration of the currently used rotation strategy.")
    public RotationStrategySummary config() {
        final IndexManagementConfig indexManagementConfig = clusterConfigService.get(IndexManagementConfig.class)
                .orElseThrow(() -> new InternalServerErrorException("Couldn't retrieve index management configuration"));

        final String strategyName = indexManagementConfig.rotationStrategy();
        final Provider<RotationStrategy> provider = rotationStrategies.get(strategyName);
        if (provider == null) {
            throw new InternalServerErrorException("Couldn't retrieve rotation strategy provider");
        }

        final RotationStrategy rotationStrategy = provider.get();
        @SuppressWarnings("unchecked")
        final Class<RotationStrategyConfig> configClass = (Class<RotationStrategyConfig>) rotationStrategy.configurationClass();
        final RotationStrategyConfig config = clusterConfigService.get(configClass)
                .orElseThrow(() -> new InternalServerErrorException("Couldn't retrieve configuration class " + configClass.getCanonicalName()));

        return RotationStrategySummary.create(strategyName, config);
    }

    @PUT
    @Path("config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    @ApiOperation(value = "Configuration of the current rotation strategy",
            notes = "This resource stores the configuration of the currently used rotation strategy.")
    public RotationStrategySummary config(@ApiParam(value = "The description of the rotation strategy and its configuration", required = true)
                                          @Valid @NotNull RotationStrategySummary rotationStrategySummary) {
        if (!rotationStrategies.containsKey(rotationStrategySummary.strategy())) {
            throw new NotFoundException("Couldn't find rotation strategy for given type " + rotationStrategySummary.strategy());
        }

        final IndexManagementConfig oldConfig = clusterConfigService.get(IndexManagementConfig.class)
                .orElseThrow(() -> new InternalServerErrorException("Couldn't retrieve index management configuration"));

        final IndexManagementConfig indexManagementConfig = IndexManagementConfig.create(
                rotationStrategySummary.strategy(),
                oldConfig.retentionStrategy()
        );

        clusterConfigService.write(rotationStrategySummary.config());
        clusterConfigService.write(indexManagementConfig);

        return rotationStrategySummary;
    }

    @GET
    @Path("strategies")
    @Timed
    @ApiOperation(value = "List available rotation strategies",
            notes = "This resource returns a list of all available rotation strategies on this Graylog node.")
    public RotationStrategies list() {
        final Set<RotationStrategyDescription> strategies = rotationStrategies.keySet()
                .stream()
                .map(this::getRotationStrategyDescription)
                .collect(Collectors.toSet());

        return RotationStrategies.create(strategies.size(), strategies);
    }

    @GET
    @Path("strategies/{strategy}")
    @Timed
    @ApiOperation(value = "Show JSON schema for configuration of given rotation strategies",
            notes = "This resource returns a JSON schema for the configuration of the given rotation strategy.")
    public RotationStrategyDescription configSchema(@ApiParam(name = "strategy", value = "The name of the rotation strategy", required = true)
                                                    @PathParam("strategy") @NotEmpty String strategyName) {
        return getRotationStrategyDescription(strategyName);
    }

    private RotationStrategyDescription getRotationStrategyDescription(String strategyName) {
        final Provider<RotationStrategy> provider = rotationStrategies.get(strategyName);
        if (provider == null) {
            throw new NotFoundException("Couldn't find rotation strategy for given type " + strategyName);
        }

        final RotationStrategy rotationStrategy = provider.get();
        final RotationStrategyConfig defaultConfig = rotationStrategy.defaultConfiguration();
        final SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
        try {
            objectMapper.acceptJsonFormatVisitor(objectMapper.constructType(rotationStrategy.configurationClass()), visitor);
        } catch (JsonMappingException e) {
            throw new InternalServerErrorException("Couldn't generate JSON schema for rotation strategy " + strategyName, e);
        }

        return RotationStrategyDescription.create(strategyName, defaultConfig, visitor.finalSchema());
    }
}
