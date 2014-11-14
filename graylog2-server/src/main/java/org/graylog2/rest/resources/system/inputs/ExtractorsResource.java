/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system.inputs;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.ConfigurationException;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.metrics.MetricUtils;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import com.wordnik.swagger.annotations.*;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.inputs.requests.CreateExtractorRequest;
import org.graylog2.rest.resources.system.inputs.requests.OrderExtractorsRequest;
import org.graylog2.security.RestPermissions;
import org.graylog2.shared.inputs.InputRegistry;
import org.graylog2.shared.system.activities.Activity;
import org.graylog2.shared.system.activities.ActivityWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

@RequiresAuthentication
@Api(value = "Extractors", description = "Extractors of an input")
@Path("/system/inputs/{inputId}/extractors")
public class ExtractorsResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(ExtractorsResource.class);

    private final InputService inputService;
    private final ActivityWriter activityWriter;
    private final InputRegistry inputs;
    private final MetricRegistry metricRegistry;
    private final ExtractorFactory extractorFactory;

    @Inject
    public ExtractorsResource(final InputService inputService,
                              final ActivityWriter activityWriter,
                              final InputRegistry inputs,
                              final MetricRegistry metricRegistry,
                              final ExtractorFactory extractorFactory) {
        this.inputService = inputService;
        this.activityWriter = activityWriter;
        this.inputs = inputs;
        this.metricRegistry = metricRegistry;
        this.extractorFactory = extractorFactory;
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Add an extractor to an input")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node."),
            @ApiResponse(code = 400, message = "No such extractor type."),
            @ApiResponse(code = 400, message = "Field the extractor should write on is reserved."),
            @ApiResponse(code = 400, message = "Missing or invalid configuration.")
    })
    public Response create(@ApiParam(name = "JSON body", required = true) String body,
                           @ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) throws NotFoundException {
        if (inputId == null || inputId.isEmpty()) {
            LOG.error("Missing inputId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
        checkPermission(RestPermissions.INPUTS_EDIT, inputId);

        MessageInput input = inputs.getRunningInput(inputId);

        if (input == null) {
            LOG.error("Input <{}> not found.", inputId);
            throw new WebApplicationException(404);
        }

        // Build extractor.
        CreateExtractorRequest cer;
        try {
            cer = objectMapper.readValue(body, CreateExtractorRequest.class);
        } catch (IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        if (cer.sourceField.isEmpty() || cer.targetField.isEmpty()) {
            LOG.error("Missing parameters. Returning HTTP 400.");
            throw new WebApplicationException(Response.Status.BAD_REQUEST);
        }

        String id = new com.eaio.uuid.UUID().toString();
        Extractor extractor;
        try {
            extractor = extractorFactory.factory(
                    id,
                    cer.title,
                    cer.order,
                    Extractor.CursorStrategy.valueOf(cer.cutOrCopy.toUpperCase()),
                    Extractor.Type.valueOf(cer.extractorType.toUpperCase()),
                    cer.sourceField,
                    cer.targetField,
                    cer.extractorConfig,
                    getCurrentUser().getName(),
                    loadConverters(cer.converters),
                    Extractor.ConditionType.valueOf(cer.conditionType.toUpperCase()),
                    cer.conditionValue
            );
        } catch (ExtractorFactory.NoSuchExtractorException e) {
            LOG.error("No such extractor type.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        } catch (Extractor.ReservedFieldException e) {
            LOG.error("Cannot create extractor. Field is reserved.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        } catch (ConfigurationException e) {
            LOG.error("Cannot create extractor. Missing configuration.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        Input mongoInput = inputService.find(input.getPersistId());
        try {
            inputService.addExtractor(mongoInput, extractor);
        } catch (ValidationException e) {
            LOG.error("Extractor persist validation failed.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        String msg = "Added extractor <" + id + "> of type [" + cer.extractorType + "] to input <" + inputId + ">.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, ExtractorsResource.class));

        Map<String, Object> result = Maps.newHashMap();
        result.put("extractor_id", id);

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @GET
    @Timed
    @ApiOperation(value = "List all extractors of an input")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public String list(@ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId) throws NotFoundException {
        if (inputId == null || inputId.isEmpty()) {
            LOG.error("Missing inputId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
        checkPermission(RestPermissions.INPUTS_READ, inputId);

        Input input = inputService.find(inputId);

        if (input == null) {
            LOG.error("Input <{}> not found.", inputId);
            throw new WebApplicationException(404);
        }

        List<Map<String, Object>> extractors = Lists.newArrayList();

        for (Extractor extractor : inputService.getExtractors(input)) {
            extractors.add(toMap(extractor));
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("extractors", extractors);
        result.put("total", inputService.getExtractors(input).size());

        return json(result);
    }

    @DELETE
    @Timed
    @ApiOperation(value = "Delete an extractor")
    @Path("/{extractorId}")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "Invalid request."),
            @ApiResponse(code = 404, message = "Input not found."),
            @ApiResponse(code = 404, message = "Extractor not found.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public void terminate(
            @ApiParam(name = "inputId", required = true) @PathParam("inputId") String inputId,
            @ApiParam(name = "extractorId", required = true) @PathParam("extractorId") String extractorId) throws NotFoundException {
        if (isNullOrEmpty(extractorId)) {
            LOG.error("extractorId is missing.");
            throw new BadRequestException("extractorId is missing.");
        }

        if (isNullOrEmpty(inputId)) {
            LOG.error("inputId is missing.");
            throw new BadRequestException("inputId is missing.");
        }
        checkPermission(RestPermissions.INPUTS_EDIT, inputId);

        final MessageInput input = inputs.getPersisted(inputId);
        if (input == null) {
            LOG.error("Input <{}> not found.", inputId);
            throw new javax.ws.rs.NotFoundException("Couldn't find input " + inputId);
        }

        // Remove from Mongo.
        final Input mongoInput = inputService.find(input.getPersistId());
        final List<Extractor> extractorList = inputService.getExtractors(mongoInput);
        final ImmutableMap<String, Extractor> idMap =
                Maps.uniqueIndex(extractorList, new Function<Extractor, String>() {
                    @Override
                    public String apply(
                            Extractor input) {
                        return input.getId();
                    }
                });
        if (!idMap.containsKey(extractorId)) {
            LOG.error("Extractor <{}> not found.", extractorId);
            throw new javax.ws.rs.NotFoundException("Couldn't find extractor " + extractorId);
        }
        inputService.removeExtractor(mongoInput, extractorId);

        final String msg = "Deleted extractor <" + extractorId + "> of type [" + idMap.get(extractorId).getType() + "] " +
                "from input <" + inputId + ">.";
        LOG.info(msg);
        activityWriter.write(new Activity(msg, InputsResource.class));
    }

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update extractor order of an input")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node.")
    })
    @Path("order")
    public Response order(@ApiParam(name = "JSON body", required = true) String body,
                          @ApiParam(name = "inputId", value = "Persist ID (!) of input.", required = true) @PathParam("inputId") String inputPersistId) throws NotFoundException {
        if (inputPersistId == null || inputPersistId.isEmpty()) {
            LOG.error("Missing inputId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }
        checkPermission(RestPermissions.INPUTS_EDIT, inputPersistId);

        Input mongoInput = inputService.find(inputPersistId);

        OrderExtractorsRequest oer;
        try {
            oer = objectMapper.readValue(body, OrderExtractorsRequest.class);
        } catch (IOException e) {
            LOG.error("Error while parsing JSON", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        for (Extractor extractor : inputService.getExtractors(mongoInput)) {
            if (oer.order.containsValue(extractor.getId())) {
                extractor.setOrder(Tools.getKeyByValue(oer.order, extractor.getId()));
            }

            // Docs embedded in MongoDB array cannot be updated atomically... :/
            inputService.removeExtractor(mongoInput, extractor.getId());
            try {
                inputService.addExtractor(mongoInput, extractor);
            } catch (ValidationException e) {
                LOG.warn("Validation error for extractor update.", e);
            }
        }

        LOG.info("Updated extractor ordering of input <persist:{}>.", inputPersistId);

        return Response.ok().build();
    }

    private Map<String, Object> toMap(Extractor extractor) {
        Map<String, Object> map = Maps.newHashMap();

        map.put(Extractor.FIELD_ID, extractor.getId());
        map.put(Extractor.FIELD_TITLE, extractor.getTitle());
        map.put(Extractor.FIELD_TYPE, extractor.getType().toString().toLowerCase());
        map.put(Extractor.FIELD_CURSOR_STRATEGY, extractor.getCursorStrategy().toString().toLowerCase());
        map.put(Extractor.FIELD_SOURCE_FIELD, extractor.getSourceField());
        map.put(Extractor.FIELD_TARGET_FIELD, extractor.getTargetField());
        map.put(Extractor.FIELD_EXTRACTOR_CONFIG, extractor.getExtractorConfig());
        map.put(Extractor.FIELD_CREATOR_USER_ID, extractor.getCreatorUserId());
        map.put(Extractor.FIELD_CONVERTERS, extractor.converterConfigMap());
        map.put(Extractor.FIELD_CONDITION_TYPE, extractor.getConditionType().toString().toLowerCase());
        map.put(Extractor.FIELD_CONDITION_VALUE, extractor.getConditionValue());
        map.put(Extractor.FIELD_ORDER, extractor.getOrder());

        map.put("exceptions", extractor.getExceptionCount());
        map.put("converter_exceptions", extractor.getConverterExceptionCount());

        Map<String, Object> metrics = Maps.newHashMap();
        metrics.put("total", MetricUtils.buildTimerMap(metricRegistry.getTimers().get(extractor.getTotalTimerName())));
        metrics.put("converters", MetricUtils.buildTimerMap(metricRegistry.getTimers().get(extractor.getConverterTimerName())));
        map.put("metrics", metrics);

        return map;
    }

    private List<Converter> loadConverters(Map<String, Map<String, Object>> requestConverters) {
        List<Converter> converters = Lists.newArrayList();

        for (Map.Entry<String, Map<String, Object>> c : requestConverters.entrySet()) {
            try {
                converters.add(ConverterFactory.factory(Converter.Type.valueOf(c.getKey().toUpperCase()), c.getValue()));
            } catch (ConverterFactory.NoSuchConverterException e) {
                LOG.warn("No such converter [" + c.getKey() + "]. Skipping.", e);
            } catch (ConfigurationException e) {
                LOG.warn("Missing configuration for [" + c.getKey() + "]. Skipping.", e);
            }
        }

        return converters;
    }

}
