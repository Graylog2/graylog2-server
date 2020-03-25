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
package org.graylog.events.rest;


import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.events.fields.providers.FieldValueProvider;
import org.graylog.events.processor.EventProcessor;
import org.graylog.events.processor.aggregation.AggregationFunction;
import org.graylog.events.processor.storage.EventStorageHandler;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Api(value = "Events/EntityTypes", description = "Event entity types")
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
                                        Map<String, EventStorageHandler.Factory> storageHandlerFactories) {
        this.eventProcessorTypes = eventProcessorFactories.keySet();
        this.fieldValueProviderTypes = fieldValueProviders.keySet();
        this.storageHandlerFactories = storageHandlerFactories.keySet();
        this.aggregationFunctions = Arrays.stream(AggregationFunction.values())
                .map(fn -> fn.name().toLowerCase(Locale.US))
                .collect(Collectors.toSet());
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
