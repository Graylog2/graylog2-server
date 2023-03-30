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
package org.graylog2.rest.resources.system.indices;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.rotation.RotationStrategy;
import org.graylog2.plugin.indexer.rotation.RotationStrategyConfig;
import org.graylog2.rest.models.system.indices.RotationStrategies;
import org.graylog2.rest.models.system.indices.RotationStrategyDescription;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.validation.constraints.NotEmpty;
import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "System/Indices/Rotation", description = "Index rotation strategy settings", tags = {CLOUD_VISIBLE})
@Path("/system/indices/rotation")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class RotationStrategyResource extends RestResource {
    private final Map<String, Provider<RotationStrategy>> rotationStrategies;
    private final ObjectMapper objectMapper;
    private final ClusterConfigService clusterConfigService;
    private final ElasticsearchConfiguration elasticsearchConfiguration;

    @Inject
    public RotationStrategyResource(Map<String, Provider<RotationStrategy>> rotationStrategies,
                                    ObjectMapper objectMapper,
                                    ClusterConfigService clusterConfigService,
                                    ElasticsearchConfiguration elasticsearchConfiguration) {
        this.rotationStrategies = requireNonNull(rotationStrategies);
        this.objectMapper = objectMapper;
        this.clusterConfigService = requireNonNull(clusterConfigService);
        this.elasticsearchConfiguration = elasticsearchConfiguration;
    }

    @GET
    @Path("strategies")
    @Timed
    @ApiOperation(value = "List available rotation strategies",
                  notes = "This resource returns a list of all available rotation strategies on this Graylog node.")
    public RotationStrategies list() {
        final List<RotationStrategyDescription> strategies = rotationStrategies.keySet()
                .stream()
                .filter(this::isEnabledRotationStrategy)
                .map(this::getRotationStrategyDescription)
                .collect(Collectors.toList());

        return RotationStrategies.create(strategies);
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

    // Limit available rotation strategies to those specified by configuration parameter enabled_index_rotation_strategies
    private boolean isEnabledRotationStrategy(String strategyName) {
        final Provider<RotationStrategy> provider = rotationStrategies.get(strategyName);
        if (provider == null) {
            throw new NotFoundException("Couldn't find rotation strategy for given type " + strategyName);
        }
        final RotationStrategy rotationStrategy = provider.get();
        return elasticsearchConfiguration.getEnabledRotationStrategies().contains(rotationStrategy.getStrategyName());
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
