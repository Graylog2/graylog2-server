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
package org.graylog2.rest.resources.system.jobs;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.scheduler.rest.JobResourceHandlerService;
import org.graylog.security.UserContext;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.system.SystemJobSummary;
import org.graylog2.rest.models.system.jobs.requests.TriggerRequest;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.system.jobs.NoSuchJobException;
import org.graylog2.system.jobs.SystemJob;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.graylog2.system.jobs.SystemJobFactory;
import org.graylog2.system.jobs.SystemJobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.inject.Inject;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@RequiresAuthentication
@Api(value = "System/Jobs", description = "System Jobs")
@Path("/system/jobs")
public class SystemJobResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(SystemJobResource.class);

    private final SystemJobFactory systemJobFactory;
    private final SystemJobManager systemJobManager;
    private final NodeId nodeId;

    private final JobResourceHandlerService jobResourceHandlerService;

    @Inject
    public SystemJobResource(SystemJobFactory systemJobFactory,
                             SystemJobManager systemJobManager,
                             NodeId nodeId,
                             JobResourceHandlerService jobResourceHandlerService) {
        this.systemJobFactory = systemJobFactory;
        this.systemJobManager = systemJobManager;
        this.nodeId = nodeId;
        this.jobResourceHandlerService = jobResourceHandlerService;
    }

    @GET
    @Timed
    @ApiOperation(value = "List currently running jobs")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<SystemJobSummary>> list() {
        final List<SystemJobSummary> jobs = Lists.newArrayListWithCapacity(systemJobManager.getRunningJobs().size());

        for (Map.Entry<String, SystemJob> entry : systemJobManager.getRunningJobs().entrySet()) {
            // TODO jobId is ephemeral, this is not a good key for permission checks. we should use the name of the job type (but there is no way to get it yet)
            if (isPermitted(RestPermissions.SYSTEMJOBS_READ, entry.getKey())) {
                final SystemJob systemJob = entry.getValue();
                jobs.add(SystemJobSummary.create(
                        systemJob.getId(),
                        systemJob.getDescription(),
                        systemJob.getClassName(),
                        systemJob.getInfo(),
                        nodeId.getNodeId(),
                        systemJob.getStartedAt(),
                        systemJob.getProgress(),
                        systemJob.isCancelable(),
                        systemJob.providesProgress()
                ));
            }
        }

        return ImmutableMap.of("jobs", jobs);
    }

    @GET
    @Timed
    @Path("/{jobId}")
    @ApiOperation(value = "Get information of a specific currently running job")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "Job not found.")
    })
    public SystemJobSummary get(@ApiParam(name = "jobId", required = true)
                                @PathParam("jobId") @NotEmpty String jobId) {
        // TODO jobId is ephemeral, this is not a good key for permission checks. we should use the name of the job type (but there is no way to get it yet)
        checkPermission(RestPermissions.SYSTEMJOBS_READ, jobId);

        SystemJob systemJob = systemJobManager.getRunningJobs().get(jobId);
        if (systemJob == null) {
            throw new NotFoundException("No system job with ID <" + jobId + "> found");
        }

        return SystemJobSummary.create(
                systemJob.getId(),
                systemJob.getDescription(),
                systemJob.getClassName(),
                systemJob.getInfo(),
                nodeId.getNodeId(),
                systemJob.getStartedAt(),
                systemJob.getProgress(),
                systemJob.isCancelable(),
                systemJob.providesProgress()
        );
    }

    @POST
    @Timed
    @ApiOperation(value = "Trigger new job")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(code = 202, message = "Job accepted."),
            @ApiResponse(code = 400, message = "There is no such systemjob type."),
            @ApiResponse(code = 403, message = "Maximum concurrency level of this systemjob type reached.")
    })
    @AuditEvent(type = AuditEventTypes.SYSTEM_JOB_START)
    public Response trigger(@ApiParam(name = "JSON body", required = true)
                            @Valid @NotNull TriggerRequest tr) {
        // TODO cleanup jobId vs jobName checking in permissions
        checkPermission(RestPermissions.SYSTEMJOBS_CREATE, tr.jobName());

        SystemJob job;
        try {
            job = systemJobFactory.build(tr.jobName());
        } catch (NoSuchJobException e) {
            LOG.error("Such a system job type does not exist. Returning HTTP 400.");
            throw new BadRequestException(e);
        }

        try {
            systemJobManager.submit(job);
        } catch (SystemJobConcurrencyException e) {
            LOG.error("Maximum concurrency level of this job reached. ", e);
            throw new ForbiddenException("Maximum concurrency level of this job reached", e);
        }

        return Response.accepted().entity(ImmutableMap.of("system_job_id", job.getId())).build();
    }

    @DELETE
    @Timed
    @Path("/{jobId}")
    @ApiOperation(value = "Cancel running job")
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.SYSTEM_JOB_STOP)
    public SystemJobSummary cancel(@ApiParam(name = "jobId", required = true) @PathParam("jobId") @NotEmpty String jobId) {
        SystemJob systemJob = systemJobManager.getRunningJobs().get(jobId);
        if (systemJob == null) {
            throw new NotFoundException("No system job with ID <" + jobId + "> found");
        }

        checkPermission(RestPermissions.SYSTEMJOBS_DELETE, systemJob.getClassName());

        if (systemJob.isCancelable()) {
            systemJob.requestCancel();
        } else {
            throw new ForbiddenException("System job with ID <" + jobId + "> cannot be cancelled");
        }

        return SystemJobSummary.create(
                systemJob.getId(),
                systemJob.getDescription(),
                systemJob.getClassName(),
                systemJob.getInfo(),
                nodeId.getNodeId(),
                systemJob.getStartedAt(),
                systemJob.getProgress(),
                systemJob.isCancelable(),
                systemJob.providesProgress()
        );
    }

    @DELETE
    @Path("/acknowledge/{jobId}")
    @ApiOperation(value = "Acknowledge job with the given ID")
    @AuditEvent(type = AuditEventTypes.SYSTEM_JOB_ACKNOWLEDGE)
    public Response acknowledgeJob(@Context UserContext userContext,
                                   @ApiParam(name = "jobId", required = true) @PathParam("jobId") @NotEmpty String jobId) {
        final int n = jobResourceHandlerService.acknowledgeJob(userContext, jobId);
        if (n < 1) {
            throw new NotFoundException("System job with ID <" + jobId + "> not found!");
        }
        return Response.accepted().build();
    }
}
