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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.audit.PipelineProcessorAuditEventTypes;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog.plugins.pipelineprocessor.db.RuleService;
import org.graylog.plugins.pipelineprocessor.events.RulesChangedEvent;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog.plugins.pipelineprocessor.parser.ParseException;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
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
import java.util.stream.Collectors;

@Api(value = "Pipelines/Rules", description = "Rules for the pipeline message processor")
@Path("/system/pipelines/rule")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class RuleResource extends RestResource implements PluginRestResource {

    private static final Logger log = LoggerFactory.getLogger(RuleResource.class);

    private final RuleService ruleService;
    private final PipelineRuleParser pipelineRuleParser;
    private final EventBus clusterBus;
    private final FunctionRegistry functionRegistry;

    @Inject
    public RuleResource(RuleService ruleService,
                        PipelineRuleParser pipelineRuleParser,
                        ClusterEventBus clusterBus,
                        FunctionRegistry functionRegistry) {
        this.ruleService = ruleService;
        this.pipelineRuleParser = pipelineRuleParser;
        this.clusterBus = clusterBus;
        this.functionRegistry = functionRegistry;
    }


    @ApiOperation(value = "Create a processing rule from source", notes = "")
    @POST
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_RULE_CREATE)
    @AuditEvent(type = PipelineProcessorAuditEventTypes.RULE_CREATE)
    public RuleSource createFromParser(@ApiParam(name = "rule", required = true) @NotNull RuleSource ruleSource) throws ParseException {
        final Rule rule;
        try {
            rule = pipelineRuleParser.parseRule(ruleSource.id(), ruleSource.source(), false);
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        final RuleDao newRuleSource = RuleDao.builder()
                .title(rule.name()) // use the name from the parsed rule source.
                .description(ruleSource.description())
                .source(ruleSource.source())
                .createdAt(DateTime.now())
                .modifiedAt(DateTime.now())
                .build();
        final RuleDao save = ruleService.save(newRuleSource);
        // TODO determine which pipelines could change because of this new rule (there could be pipelines referring to a previously unresolved rule)
        clusterBus.post(RulesChangedEvent.updatedRuleId(save.id()));
        log.debug("Created new rule {}", save);
        return RuleSource.fromDao(pipelineRuleParser, save);
    }

    @ApiOperation(value = "Parse a processing rule without saving it", notes = "")
    @POST
    @Path("/parse")
    @NoAuditEvent("only used to parse a rule, no changes made in the system")
    public RuleSource parse(@ApiParam(name = "rule", required = true) @NotNull RuleSource ruleSource) throws ParseException {
        final Rule rule;
        try {
            // be silent about parse errors here, many requests will result in invalid syntax
            rule = pipelineRuleParser.parseRule(ruleSource.id(), ruleSource.source(), true);
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        return RuleSource.builder()
                .title(rule.name())
                .description(ruleSource.description())
                .source(ruleSource.source())
                .createdAt(DateTime.now())
                .modifiedAt(DateTime.now())
                .build();
    }

    @ApiOperation(value = "Get all processing rules")
    @GET
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_RULE_READ)
    public Collection<RuleSource> getAll() {
        final Collection<RuleDao> ruleDaos = ruleService.loadAll();
        return ruleDaos.stream()
                .map(ruleDao -> RuleSource.fromDao(pipelineRuleParser, ruleDao))
                .collect(Collectors.toList());
    }

    @ApiOperation(value = "Get a processing rule", notes = "It can take up to a second until the change is applied")
    @Path("/{id}")
    @GET
    public RuleSource get(@ApiParam(name = "id") @PathParam("id") String id) throws NotFoundException {
        checkPermission(PipelineRestPermissions.PIPELINE_RULE_READ, id);
        return RuleSource.fromDao(pipelineRuleParser, ruleService.load(id));
    }

    @ApiOperation("Retrieve the named processing rules in bulk")
    @Path("/multiple")
    @POST
    @NoAuditEvent("only used to get multiple pipeline rules")
    public Collection<RuleSource> getBulk(@ApiParam("rules") BulkRuleRequest rules) {
        Collection<RuleDao> ruleDaos = ruleService.loadNamed(rules.rules());

        return ruleDaos.stream()
                .map(ruleDao -> RuleSource.fromDao(pipelineRuleParser, ruleDao))
                .filter(rule -> isPermitted(PipelineRestPermissions.PIPELINE_RULE_READ, rule.id()))
                .collect(Collectors.toList());
    }

    @ApiOperation(value = "Modify a processing rule", notes = "It can take up to a second until the change is applied")
    @Path("/{id}")
    @PUT
    @AuditEvent(type = PipelineProcessorAuditEventTypes.RULE_UPDATE)
    public RuleSource update(@ApiParam(name = "id") @PathParam("id") String id,
                             @ApiParam(name = "rule", required = true) @NotNull RuleSource update) throws NotFoundException {
        checkPermission(PipelineRestPermissions.PIPELINE_RULE_EDIT, id);

        final RuleDao ruleDao = ruleService.load(id);
        final Rule rule;
        try {
            rule = pipelineRuleParser.parseRule(id, update.source(), false);
        } catch (ParseException e) {
            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(e.getErrors()).build());
        }
        final RuleDao toSave = ruleDao.toBuilder()
                .title(rule.name())
                .description(update.description())
                .source(update.source())
                .modifiedAt(DateTime.now())
                .build();
        final RuleDao savedRule = ruleService.save(toSave);

        // TODO determine which pipelines could change because of this updated rule
        clusterBus.post(RulesChangedEvent.updatedRuleId(savedRule.id()));

        return RuleSource.fromDao(pipelineRuleParser, savedRule);
    }

    @ApiOperation(value = "Delete a processing rule", notes = "It can take up to a second until the change is applied")
    @Path("/{id}")
    @DELETE
    @AuditEvent(type = PipelineProcessorAuditEventTypes.RULE_DELETE)
    public void delete(@ApiParam(name = "id") @PathParam("id") String id) throws NotFoundException {
        checkPermission(PipelineRestPermissions.PIPELINE_RULE_DELETE, id);
        ruleService.load(id);
        ruleService.delete(id);

        // TODO determine which pipelines could change because of this deleted rule, causing them to recompile
        clusterBus.post(RulesChangedEvent.deletedRuleId(id));
    }


    @ApiOperation("Get function descriptors")
    @Path("/functions")
    @GET
    public Collection<Object> functionDescriptors() {
        return functionRegistry.all().stream()
                .map(Function::descriptor)
                .collect(Collectors.toList());
    }

}
