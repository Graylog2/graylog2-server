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

import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.elasticsearch.common.Strings;
import org.graylog.plugins.pipelineprocessor.processors.ConfigurationStateUpdater;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.simulator.PipelineInterpreterTracer;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.streams.StreamService;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;

@Api(value = "Pipelines/Simulator", description = "Simulate pipeline message processor")
@Path("/system/pipelines/simulate")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class SimulatorResource extends RestResource implements PluginRestResource {
    private final ConfigurationStateUpdater pipelineStateUpdater;
    private final StreamService streamService;
    private final PipelineInterpreter pipelineInterpreter;

    @Inject
    public SimulatorResource(PipelineInterpreter pipelineInterpreter,
                             ConfigurationStateUpdater pipelineStateUpdater,
                             StreamService streamService) {
        this.pipelineInterpreter = pipelineInterpreter;
        this.pipelineStateUpdater = pipelineStateUpdater;
        this.streamService = streamService;
    }

    @ApiOperation(value = "Simulate the execution of the pipeline message processor")
    @POST
    @RequiresPermissions(PipelineRestPermissions.PIPELINE_RULE_READ)
    @NoAuditEvent("only used to test pipelines, no changes made in the system")
    public SimulationResponse simulate(@ApiParam(name = "simulation", required = true) @NotNull SimulationRequest request) throws NotFoundException {
        checkPermission(RestPermissions.STREAMS_READ, request.streamId());

        final Message message = new Message(request.message());
        final Stream stream = streamService.load(request.streamId());
        message.addStream(stream);

        if (!Strings.isNullOrEmpty(request.inputId())) {
            message.setSourceInputId(request.inputId());
        }

        final List<ResultMessageSummary> simulationResults = new ArrayList<>();
        final PipelineInterpreterTracer pipelineInterpreterTracer = new PipelineInterpreterTracer();

        org.graylog2.plugin.Messages processedMessages = pipelineInterpreter.process(message,
                                                                                     pipelineInterpreterTracer.getSimulatorInterpreterListener(),
                                                                                     pipelineStateUpdater.getLatestState());
        for (Message processedMessage : processedMessages) {
            simulationResults.add(ResultMessageSummary.create(null, processedMessage.getFields(), ""));
        }

        return SimulationResponse.create(simulationResults,
                                         pipelineInterpreterTracer.getExecutionTrace(),
                                         pipelineInterpreterTracer.took());
    }
}
