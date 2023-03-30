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
import com.fasterxml.jackson.module.jsonSchema.JsonSchema;
import com.fasterxml.jackson.module.jsonSchema.factories.SchemaFactoryWrapper;
import com.fasterxml.jackson.module.jsonSchema.types.ObjectSchema;
import com.fasterxml.jackson.module.jsonSchema.types.StringSchema;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.plugin.indexer.retention.RetentionStrategy;
import org.graylog2.plugin.indexer.retention.RetentionStrategyConfig;
import org.graylog2.rest.models.system.indices.RetentionStrategies;
import org.graylog2.rest.models.system.indices.RetentionStrategyDescription;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "System/Indices/Retention", description = "Index retention strategy settings", tags = {CLOUD_VISIBLE})
@Path("/system/indices/retention")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class RetentionStrategyResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(RetentionStrategyResource.class);

    private final Map<String, Provider<RetentionStrategy>> retentionStrategies;
    private final ObjectMapper objectMapper;
    private final ElasticsearchConfiguration elasticsearchConfiguration;

    @Inject
    public RetentionStrategyResource(Map<String, Provider<RetentionStrategy>> retentionStrategies,
                                     ObjectMapper objectMapper,
                                     ElasticsearchConfiguration elasticsearchConfiguration) {
        this.retentionStrategies = requireNonNull(retentionStrategies);
        this.objectMapper = objectMapper;
        this.elasticsearchConfiguration = elasticsearchConfiguration;
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

        final RetentionStrategies.Context context =
                RetentionStrategies.Context.create(elasticsearchConfiguration.getMaxIndexRetentionPeriod());

        return RetentionStrategies.create(strategies.size(), strategies, context);
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

        JsonSchema jsonSchema = visitor.finalSchema();
        removeDeactivatedStrategiesActions(jsonSchema);

        return RetentionStrategyDescription.create(strategyName, defaultConfig, jsonSchema);
    }

    private void removeDeactivatedStrategiesActions(JsonSchema schema) {
        Map<String, JsonSchema> properties = ((ObjectSchema) schema).getProperties();
        JsonSchema indexAction = properties.get("index_action");

        if (Objects.nonNull(indexAction)) {
            Set<String> actionEnums = ((StringSchema) indexAction).getEnums();
            Set<String> disabledRetentionStrategies = elasticsearchConfiguration.getDisabledRetentionStrategies();
            disabledRetentionStrategies.stream().map(s -> s.toUpperCase(Locale.ENGLISH)).toList().forEach(actionEnums::remove);
        }
    }
}
