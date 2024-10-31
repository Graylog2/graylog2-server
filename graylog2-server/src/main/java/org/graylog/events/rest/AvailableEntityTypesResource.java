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
package org.graylog.events.rest;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.events.fields.providers.FieldValueProvider;
import org.graylog.events.processor.EventProcessor;
import org.graylog.events.processor.storage.EventStorageHandler;
import org.graylog.plugins.views.search.rest.SeriesDescription;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import jakarta.inject.Inject;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.Map;
import java.util.Set;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Events/EntityTypes", description = "Event entity types", tags = {CLOUD_VISIBLE})
@Path("/events/entity_types")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class AvailableEntityTypesResource extends RestResource implements PluginRestResource {
    private final Set<String> eventProcessorTypes;
    private final Set<String> fieldValueProviderTypes;
    private final Set<String> storageHandlerFactories;
    private final Set<String> aggregationFunctions;

    @Inject
    public AvailableEntityTypesResource(Map<String, EventProcessor.Factory> eventProcessorFactories,
                                        Map<String, FieldValueProvider.Factory> fieldValueProviders,
                                        Map<String, EventStorageHandler.Factory> storageHandlerFactories,
                                        Map<String, SeriesDescription> aggregationFunctions) {
        this.eventProcessorTypes = eventProcessorFactories.keySet();
        this.fieldValueProviderTypes = fieldValueProviders.keySet();
        this.storageHandlerFactories = storageHandlerFactories.keySet();
        this.aggregationFunctions = aggregationFunctions.keySet();
    }

    @GET
    @ApiOperation("List all available entity types")
    public AvailableEntityTypesSummary all() {
        return AvailableEntityTypesSummary.create(eventProcessorTypes, fieldValueProviderTypes, storageHandlerFactories, aggregationFunctions);
    }

    @AutoValue
    @JsonAutoDetect
    public static abstract class AvailableEntityTypesSummary {
        @JsonProperty("processor_types")
        public abstract Set<String> processorTypes();

        @JsonProperty("field_provider_types")
        public abstract Set<String> fieldProviderTypes();

        @JsonProperty("storage_handler_types")
        public abstract Set<String> storageHandlerTypes();

        @JsonProperty("aggregation_functions")
        public abstract Set<String> aggregationFunctions();

        public static AvailableEntityTypesSummary create(Set<String> processorTypes,
                                                         Set<String> fieldProviderTypes,
                                                         Set<String> storageHandlerTypes,
                                                         Set<String> aggregationFunctions) {
            return new AutoValue_AvailableEntityTypesResource_AvailableEntityTypesSummary(processorTypes, fieldProviderTypes, storageHandlerTypes, aggregationFunctions);
        }
    }
}
