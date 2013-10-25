/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
 *
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
 *
 */
package org.graylog2.rest.resources.system.inputs;

import com.beust.jcommander.internal.Lists;
import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Maps;
import org.graylog2.ConfigurationException;
import org.graylog2.database.ValidationException;
import org.graylog2.inputs.Input;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.documentation.annotations.*;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.inputs.requests.CreateExtractorRequest;
import org.graylog2.system.activities.Activity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@Api(value = "Extractors", description = "Extractors of an input")
@Path("/system/inputs/{inputId}/extractors")
public class ExtractorsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(ExtractorsResource.class);

    @POST @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Add an extractor to an input")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node."),
            @ApiResponse(code = 400, message = "No such extractor type."),
            @ApiResponse(code = 400, message = "Field the extractor should write on is reserved."),
            @ApiResponse(code = 400, message = "Missing or invalid configuration.")
    })
    public Response create(@ApiParam(title = "JSON body", required = true) String body,
                           @ApiParam(title = "inputId", required = true) @PathParam("inputId") String inputId) {
        if (inputId == null || inputId.isEmpty()) {
            LOG.error("Missing inputId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        MessageInput input = core.inputs().getRunningInputs().get(inputId);

        if (input == null) {
            LOG.error("Input <{}> not found.", inputId);
            throw new WebApplicationException(404);
        }

        // Build extractor.
        CreateExtractorRequest cer;
        try {
            cer = objectMapper.readValue(body, CreateExtractorRequest.class);
        } catch(IOException e) {
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
            extractor = ExtractorFactory.factory(
                    id,
                    cer.title,
                    Extractor.CursorStrategy.valueOf(cer.cutOrCopy.toUpperCase()),
                    Extractor.Type.valueOf(cer.extractorType.toUpperCase()),
                    cer.sourceField,
                    cer.targetField,
                    cer.extractorConfig,
                    cer.creatorUserId,
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

        input.addExtractor(id, extractor);

        Input mongoInput = Input.find(core, input.getPersistId());
        try {
            mongoInput.addExtractor(extractor);
        } catch (ValidationException e) {
            LOG.error("Extractor persist validation failed.", e);
            throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
        }

        String msg = "Added extractor <" + id + "> of type [" + cer.extractorType + "] to input <" + inputId + ">.";
        LOG.info(msg);
        core.getActivityWriter().write(new Activity(msg, ExtractorsResource.class));

        Map<String, Object> result = Maps.newHashMap();
        result.put("extractor_id", id);

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @GET @Timed
    @ApiOperation(value = "List all extractors of an input")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public String list(@ApiParam(title = "inputId", required = true) @PathParam("inputId") String inputId) {
        if (inputId == null || inputId.isEmpty()) {
            LOG.error("Missing inputId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        MessageInput input = core.inputs().getRunningInputs().get(inputId);

        if (input == null) {
            LOG.error("Input <{}> not found.", inputId);
            throw new WebApplicationException(404);
        }

        List<Map<String, Object>> extractors = Lists.newArrayList();

        for (Extractor extractor : input.getExtractors().values()) {
            extractors.add(toMap(extractor));
        }

        Map<String, Object> result = Maps.newHashMap();
        result.put("extractors", extractors);
        result.put("total", input.getExtractors().size());

        return json(result);
    }

    @DELETE @Timed
    @ApiOperation(value = "Delete an extractor")
    @Path("/{extractorId}")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No such input on this node."),
            @ApiResponse(code = 404, message = "Extractor not found.")
    })
    @Produces(MediaType.APPLICATION_JSON)
    public Response terminate(
            @ApiParam(title = "inputId", required = true) @PathParam("inputId") String inputId,
            @ApiParam(title = "extractorId", required = true) @PathParam("extractorId") String extractorId) {
        if (extractorId == null || extractorId.isEmpty()) {
            LOG.error("Missing extractorId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        if (inputId == null || inputId.isEmpty()) {
            LOG.error("Missing inputId. Returning HTTP 400.");
            throw new WebApplicationException(400);
        }

        MessageInput input = core.inputs().getRunningInputs().get(inputId);

        if (input == null) {
            LOG.error("Input <{}> not found.", inputId);
            throw new WebApplicationException(404);
        }

        if (input.getExtractors().get(extractorId) == null) {
            LOG.error("Extractor <{}> not found.", extractorId);
            throw new WebApplicationException(404);
        }

        // Remove from Mongo.
        Input mongoInput = Input.find(core, input.getPersistId());
        mongoInput.removeExtractor(extractorId);

        Extractor extractor = input.getExtractors().get(extractorId);
        input.getExtractors().remove(extractorId);

        String msg = "Deleted extractor <" + extractorId + "> of type [" + extractor.getType() + "] " +
                "from input <" + inputId + ">. Reason: REST request.";
        LOG.info(msg);
        core.getActivityWriter().write(new Activity(msg, InputsResource.class));

        return Response.status(Response.Status.NO_CONTENT).build();
    }

    private Map<String, Object> toMap(Extractor extractor) {
        Map<String, Object> map = Maps.newHashMap();

        map.put("id", extractor.getId());
        map.put("title", extractor.getTitle());
        map.put("type", extractor.getType().toString().toLowerCase());
        map.put("cursor_strategy", extractor.getCursorStrategy().toString().toLowerCase());
        map.put("source_field", extractor.getSourceField());
        map.put("target_field", extractor.getTargetField());
        map.put("extractor_config", extractor.getExtractorConfig());
        map.put("creator_user_id", extractor.getCreatorUserId());
        map.put("converters", extractor.converterConfigMap());
        map.put("condition_type", extractor.getConditionType().toString().toLowerCase());
        map.put("condition_value", extractor.getConditionValue());

        map.put("exceptions", extractor.getExceptionCount());
        map.put("converter_exceptions", extractor.getConverterExceptionCount());

        Map<String, Object> metrics = Maps.newHashMap();
        metrics.put("total",  buildTimerMap(core.metrics().getTimers().get(extractor.getTotalTimerName())));
        metrics.put("converters", buildTimerMap(core.metrics().getTimers().get(extractor.getConverterTimerName())));
        map.put("metrics", metrics);

        return map;
    }

    private List<Converter> loadConverters(Map<String, Map<String, Object>> requestConverters) {
        List<Converter> converters = Lists.newArrayList();

        for (Map.Entry<String, Map<String, Object>> c : requestConverters.entrySet()) {
            try {
                converters.add(ConverterFactory.factory(Converter.Type.valueOf(c.getKey().toUpperCase()), c.getValue()));
            } catch (ConverterFactory.NoSuchConverterException e) {
                LOG.warn("No such converter [{}]. Skipping.", c.getKey(), e);
            } catch (ConfigurationException e1) {
                LOG.warn("Missing configuration for [{}]. Skipping.", c.getKey(), e1);
            }
        }

        return converters;
    }

}
