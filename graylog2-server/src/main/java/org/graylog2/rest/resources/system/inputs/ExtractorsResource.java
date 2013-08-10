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
import org.elasticsearch.common.UUID;
import org.graylog2.ConfigurationException;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.rest.resources.RestResource;
import org.graylog2.rest.resources.system.inputs.requests.CreateExtractorRequest;
import org.graylog2.plugin.inputs.Extractor;
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
@Path("/system/inputs/{inputId}/extractors")
public class ExtractorsResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(InputsResource.class);

    @POST
    @Timed
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response create(String body, @PathParam("inputId") String inputId, @QueryParam("pretty") boolean prettyPrint) {
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

        String id = UUID.randomUUID().toString();
        Extractor extractor;
        try {
            extractor = ExtractorFactory.factory(
                    id,
                    Extractor.CursorStrategy.valueOf(cer.cutOrCopy.toUpperCase()),
                    Extractor.Type.valueOf(cer.extractorType.toUpperCase()),
                    cer.sourceField,
                    cer.targetField,
                    cer.extractorConfig
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

        // TODO: Persist extractor. (add to mongo input)

        String msg = "Added extractor <" + id + "> of type [" + cer.extractorType + "] to input <" + inputId + ">.";
        LOG.info(msg);
        core.getActivityWriter().write(new Activity(msg, ExtractorsResource.class));

        Map<String, Object> result = Maps.newHashMap();
        result.put("extractor_id", id);

        return Response.status(Response.Status.CREATED).entity(json(result)).build();
    }

    @GET
    @Timed
    @Produces(MediaType.APPLICATION_JSON)
    public String list(@PathParam("inputId") String inputId, @QueryParam("pretty") boolean prettyPrint) {
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

    private Map<String, Object> toMap(Extractor extractor) {
        Map<String, Object> map = Maps.newHashMap();

        map.put("id", extractor.getId());
        map.put("type", extractor.getType().toString().toLowerCase());
        map.put("cursor_strategy", extractor.getCursorStrategy().toString().toLowerCase());
        map.put("source_field", extractor.getSourceField());
        map.put("target_field", extractor.getTargetField());
        map.put("extractor_config", extractor.getExtractorConfig());

        return map;
    }

}
