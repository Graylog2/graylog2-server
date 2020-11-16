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
package org.graylog.plugins.pipelineprocessor.rest;

import com.google.common.base.Strings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
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

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

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
