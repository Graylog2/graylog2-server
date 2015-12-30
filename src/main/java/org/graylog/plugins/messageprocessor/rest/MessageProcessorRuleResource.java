package org.graylog.plugins.messageprocessor.rest;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.graylog.plugins.messageprocessor.db.RuleSourceService;
import org.graylog.plugins.messageprocessor.parser.ParseException;
import org.graylog.plugins.messageprocessor.parser.RuleParser;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Api(value = "MessageProcessing", description = "Message processing pipeline")
@Path("/messageprocessors")
public class MessageProcessorRuleResource extends RestResource implements PluginRestResource {

    private static final Logger log = LoggerFactory.getLogger(MessageProcessorRuleResource.class);

    private final RuleSourceService ruleSourceService;
    private final RuleParser ruleParser;

    @Inject
    public MessageProcessorRuleResource(RuleSourceService ruleSourceService, RuleParser ruleParser) {
        this.ruleSourceService = ruleSourceService;
        this.ruleParser = ruleParser;
    }


    @ApiOperation(value = "Create a processing rule from source", notes = "")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/rule")
    @POST
    public RuleSource createFromParser(@ApiParam(name = "processingRule", required = true) @NotNull String ruleSource) throws ParseException {
        try {
            ruleParser.parseRule(ruleSource);
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        final RuleSource newRuleSource = RuleSource.builder()
                .source(ruleSource)
                .createdAt(DateTime.now())
                .modifiedAt(DateTime.now())
                .build();
        final RuleSource save = ruleSourceService.save(newRuleSource);
        log.info("Created new rule {}", save);
        return save;
    }

    @ApiOperation(value = "Get a processing rule", notes = "It can take up to a second until the change is applied")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/rule/{id}")
    @GET
    public RuleSource get(@ApiParam(name = "id") @PathParam("id") String id) throws NotFoundException {
        return ruleSourceService.load(id);
    }

    @ApiOperation(value = "Modify a processing rule", notes = "It can take up to a second until the change is applied")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/rule/{id}")
    @PUT
    public Response update(@ApiParam(name = "id") @PathParam("id") String id) {
        // TODO
        return Response.ok().build();
    }

    @ApiOperation(value = "Delete a processing rule", notes = "It can take up to a second until the change is applied")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/rule/{id}")
    @DELETE
    public void delete(@ApiParam(name = "id") @PathParam("id") String id) {
        // TODO
    }


}
