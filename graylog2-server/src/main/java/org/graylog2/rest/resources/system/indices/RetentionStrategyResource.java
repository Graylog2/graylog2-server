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
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.rest.models.system.indices.RetentionStrategies;
import org.graylog2.rest.models.system.indices.RetentionStrategyDescription;
import org.graylog2.rest.models.system.indices.RetentionStrategySummary;
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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

@Api(value = "System/Indices/Retention", description = "Index retention strategy settings")
@Path("/system/indices/retention")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class RetentionStrategyResource extends RestResource {
    private final Map<String, Provider<RetentionStrategy>> retentionStrategies;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public RetentionStrategyResource(Map<String, Provider<RetentionStrategy>> retentionStrategies,
                                     ClusterConfigService clusterConfigService) {
        this.retentionStrategies = requireNonNull(retentionStrategies);
        this.clusterConfigService = requireNonNull(clusterConfigService);
    }

    @GET
    @Path("config")
    @Timed
    @ApiOperation(value = "Configuration of the current retention strategy",
            notes = "This resource returns the configuration of the currently used retention strategy.")
    public RetentionStrategySummary config() {
        final IndexManagementConfig config = clusterConfigService.get(IndexManagementConfig.class)
                .orElseThrow(() -> new InternalServerErrorException("Couldn't retrieve index management configuration"));

        final String strategyName = config.retentionStrategy();
        final Provider<RetentionStrategy> provider = retentionStrategies.get(strategyName);
        if (provider == null) {
            throw new InternalServerErrorException("Couldn't retrieve retention strategy provider");
        }

        final RetentionStrategy retentionStrategy = provider.get();
        @SuppressWarnings("unchecked")
        final Class<RetentionStrategyConfig> configClass = (Class<RetentionStrategyConfig>) retentionStrategy.configurationClass();
        final RetentionStrategyConfig retentionStrategyConfig = clusterConfigService.get(configClass)
                .orElseThrow(() -> new InternalServerErrorException("Couldn't retrieve configuration class " + configClass.getCanonicalName()));

        return RetentionStrategySummary.create(strategyName, retentionStrategyConfig);
    }

    @PUT
    @Path("config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Timed
    @ApiOperation(value = "Configuration of the current retention strategy",
            notes = "This resource stores the configuration of the currently used retention strategy.")
    public RetentionStrategySummary config(@ApiParam(value = "The description of the retention strategy and its configuration", required = true)
                                           @Valid @NotNull RetentionStrategySummary retentionStrategySummary) {
        if (!retentionStrategies.containsKey(retentionStrategySummary.strategy())) {
            throw new NotFoundException("Couldn't find retention strategy for given type " + retentionStrategySummary.strategy());
        }

        final Optional<IndexManagementConfig> oldIndexManagementConfig = clusterConfigService.get(IndexManagementConfig.class);
        final IndexManagementConfig oldConfig = oldIndexManagementConfig.orElseThrow(
                () -> new InternalServerErrorException("Couldn't retrieve index management configuration"));

        final IndexManagementConfig indexManagementConfig = IndexManagementConfig.create(
                oldConfig.rotationStrategy(),
                retentionStrategySummary.strategy()
        );

        clusterConfigService.write(retentionStrategySummary.config());
        clusterConfigService.write(indexManagementConfig);

        return retentionStrategySummary;
    }

    @GET
    @Path("strategies")
    @Timed
    @ApiOperation(value = "List available retention strategies",
            notes = "This resource returns a list of all available retention strategies on this Graylog node.")
    public RetentionStrategies list() {
        final Set<RetentionStrategyDescription> strategies = retentionStrategies.keySet()
                .stream()
                .map(this::getRetentionStrategyDescription)
                .collect(Collectors.toSet());

        return RetentionStrategies.create(strategies.size(), strategies);
    }

    @GET
    @Path("strategies/{strategy}")
    @Timed
    @ApiOperation(value = "Show JSON schema for configuration of given retention strategies",
            notes = "This resource returns a JSON schema for the configuration of the given retention strategy.")
    public RetentionStrategyDescription configSchema(@ApiParam(name = "strategy", value = "The name of the retention strategy", required = true)
                                                     @PathParam("strategy") @NotEmpty String strategyName) {
        return getRetentionStrategyDescription(strategyName);
    }

    private RetentionStrategyDescription getRetentionStrategyDescription(@ApiParam(name = "strategy", value = "The name of the retention strategy", required = true) @PathParam("strategy") @NotEmpty String strategyName) {
        final Provider<RetentionStrategy> provider = retentionStrategies.get(strategyName);
        if (provider == null) {
            throw new NotFoundException("Couldn't find retention strategy for given type " + strategyName);
        }

        final RetentionStrategy retentionStrategy = provider.get();
        final RetentionStrategyConfig defaultConfig = retentionStrategy.defaultConfiguration();
        final SchemaFactoryWrapper visitor = new SchemaFactoryWrapper();
        try {
            objectMapper.acceptJsonFormatVisitor(objectMapper.constructType(retentionStrategy.configurationClass()), visitor);
        } catch (JsonMappingException e) {
            throw new InternalServerErrorException("Couldn't generate JSON schema for retention strategy " + strategyName, e);
        }

        return RetentionStrategyDescription.create(strategyName, defaultConfig, visitor.finalSchema());
    }
}
