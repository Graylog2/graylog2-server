/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.rest;

import com.google.common.eventbus.EventBus;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.graylog.plugins.pipelineprocessor.db.RuleSourceService;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
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
import java.util.Collection;

@Api(value = "Pipeline/Rules", description = "Rules for the pipeline message processor")
@Path("/system/pipelines")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class RuleResource extends RestResource implements PluginRestResource {

    private static final Logger log = LoggerFactory.getLogger(RuleResource.class);

    private final RuleSourceService ruleSourceService;
    private final PipelineRuleParser pipelineRuleParser;
    private final EventBus clusterBus;

    @Inject
    public RuleResource(RuleSourceService ruleSourceService,
                        PipelineRuleParser pipelineRuleParser,
                        @ClusterEventBus EventBus clusterBus) {
        this.ruleSourceService = ruleSourceService;
        this.pipelineRuleParser = pipelineRuleParser;
        this.clusterBus = clusterBus;
    }


    @ApiOperation(value = "Create a processing rule from source", notes = "")
    @POST
    @Path("/rule")
    public RuleSource createFromParser(@ApiParam(name = "rule", required = true) @NotNull RuleSource ruleSource) throws ParseException {
        try {
            pipelineRuleParser.parseRule(ruleSource.source());
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        final RuleSource newRuleSource = RuleSource.builder()
                .title(ruleSource.title())
                .description(ruleSource.description())
                .source(ruleSource.source())
                .createdAt(DateTime.now())
                .modifiedAt(DateTime.now())
                .build();
        final RuleSource save = ruleSourceService.save(newRuleSource);
        // TODO determine which pipelines could change because of this new rule (there could be pipelines referring to a previously unresolved rule)
        clusterBus.post(RulesChangedEvent.updatedRuleId(save.id()));
        log.info("Created new rule {}", save);
        return save;
    }

    @ApiOperation(value = "Parse a processing rule without saving it", notes = "")
    @POST
    @Path("/rule/parse")
    public RuleSource parse(@ApiParam(name = "rule", required = true) @NotNull RuleSource ruleSource) throws ParseException {
        try {
            pipelineRuleParser.parseRule(ruleSource.source());
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        return RuleSource.builder()
                .title(ruleSource.title())
                .description(ruleSource.description())
                .source(ruleSource.source())
                .createdAt(DateTime.now())
                .modifiedAt(DateTime.now())
                .build();
    }

    @ApiOperation(value = "Get all processing rules")
    @GET
    @Path("/rule")
    public Collection<RuleSource> getAll() {
        return  ruleSourceService.loadAll();
    }

    @ApiOperation(value = "Get a processing rule", notes = "It can take up to a second until the change is applied")
    @Path("/rule/{id}")
    @GET
    public RuleSource get(@ApiParam(name = "id") @PathParam("id") String id) throws NotFoundException {
        return ruleSourceService.load(id);
    }

    @ApiOperation(value = "Modify a processing rule", notes = "It can take up to a second until the change is applied")
    @Path("/rule/{id}")
    @PUT
    public RuleSource update(@ApiParam(name = "id") @PathParam("id") String id,
                             @ApiParam(name = "rule", required = true) @NotNull RuleSource update) throws NotFoundException {
        final RuleSource ruleSource = ruleSourceService.load(id);
        try {
            pipelineRuleParser.parseRule(update.source());
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        final RuleSource toSave = ruleSource.toBuilder()
                .source(update.source())
                .modifiedAt(DateTime.now())
                .build();
        final RuleSource savedRule = ruleSourceService.save(toSave);

        // TODO determine which pipelines could change because of this updated rule
        clusterBus.post(RulesChangedEvent.updatedRuleId(savedRule.id()));

        return savedRule;
    }

    @ApiOperation(value = "Delete a processing rule", notes = "It can take up to a second until the change is applied")
    @Path("/rule/{id}")
    @DELETE
    public void delete(@ApiParam(name = "id") @PathParam("id") String id) throws NotFoundException {
        ruleSourceService.load(id);
        ruleSourceService.delete(id);

        // TODO determine which pipelines could change because of this deleted rule, causing them to recompile
        clusterBus.post(RulesChangedEvent.deletedRuleId(id));
    }

}
