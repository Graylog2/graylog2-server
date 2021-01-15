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
package org.graylog.scheduler.rest;


import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.scheduler.DBJobDefinitionService;
import org.graylog.scheduler.DBJobTriggerService;
import org.graylog.scheduler.JobDefinitionDto;
import org.graylog.scheduler.JobTriggerDto;
import org.graylog.scheduler.audit.JobSchedulerAuditEventTypes;
import org.graylog.scheduler.rest.requests.CreateJobTriggerRequest;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.util.List;

@Api(value = "JobScheduler", description = "Scheduler management")
@Path("/scheduler")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class JobSchedulerResource extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(JobSchedulerResource.class);

    private final DBJobTriggerService dbJobTriggerService;
    private final DBJobDefinitionService dbJobDefinitionService;

    @Inject
    public JobSchedulerResource(DBJobTriggerService dbJobTriggerService, DBJobDefinitionService dbJobDefinitionService) {
        this.dbJobTriggerService = dbJobTriggerService;
        this.dbJobDefinitionService = dbJobDefinitionService;
    }

    @GET
    @Path("/jobs")
    @ApiOperation("List all available jobs")
    public PaginatedResponse<JobDefinitionDto> listJobs(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                        @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage) {
        return PaginatedResponse.create("job_definitions", dbJobDefinitionService.getAllPaginated(JobDefinitionDto.FIELD_TITLE, page, perPage));
    }

    @GET
    @Path("/jobs/{jobDefinitionId}")
    @ApiOperation("Get a job definition ")
    public JobDefinitionDto get(@ApiParam(name = "jobDefinitionId") @PathParam("jobDefinitionId") @NotBlank String jobDefinitionId) {
        return dbJobDefinitionService.get(jobDefinitionId)
                .orElseThrow(() -> new NotFoundException("Job definition " + jobDefinitionId + " doesn't exist"));
    }

    @POST
    @Path("/jobs")
    @ApiOperation("Create new job definition")
    @AuditEvent(type = JobSchedulerAuditEventTypes.SCHEDULER_JOB_CREATE)
    public JobDefinitionDto create(JobDefinitionDto dto) {
        return dbJobDefinitionService.save(dto);
    }

    @PUT
    @Path("/jobs/{jobDefinitionId}")
    @ApiOperation("Update existing job definition")
    @AuditEvent(type = JobSchedulerAuditEventTypes.SCHEDULER_JOB_UPDATE)
    public JobDefinitionDto update(@ApiParam(name = "jobDefinitionId") @PathParam("jobDefinitionId") @NotBlank String jobDefinitionId,
                                   JobDefinitionDto dto) {
        dbJobDefinitionService.get(jobDefinitionId)
                .orElseThrow(() -> new NotFoundException("Job definition " + jobDefinitionId + " doesn't exist"));

        if (!jobDefinitionId.equals(dto.id())) {
            throw new BadRequestException("Job definition IDs don't match");
        }

        return dbJobDefinitionService.save(dto);
    }

    @DELETE
    @Path("/jobs/{jobDefinitionId}")
    @ApiOperation("Delete job definition")
    @AuditEvent(type = JobSchedulerAuditEventTypes.SCHEDULER_JOB_DELETE)
    public void delete(@ApiParam(name = "jobDefinitionId") @PathParam("jobDefinitionId") @NotBlank String jobDefinitionId) {
        dbJobDefinitionService.delete(jobDefinitionId);
    }

    @GET
    @Path("/triggers")
    @ApiOperation("List all available job triggers")
    public PaginatedResponse<JobTriggerDto> listTriggers() {
        final List<JobTriggerDto> triggers = dbJobTriggerService.all();

        return PaginatedResponse.create("triggers", new PaginatedList<>(triggers, triggers.size(), 1, 50));
    }

    @POST
    @Path("/triggers")
    @ApiOperation("Create new job trigger")
    @AuditEvent(type = JobSchedulerAuditEventTypes.SCHEDULER_TRIGGER_CREATE)
    public JobTriggerDto createTrigger(@Valid CreateJobTriggerRequest request) {
        try {
            return dbJobTriggerService.create(request.toDto());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }
}
