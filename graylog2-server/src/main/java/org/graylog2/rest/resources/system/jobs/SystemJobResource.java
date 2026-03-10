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
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.collect.ImmutableMap;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.scheduler.rest.JobResourceHandlerService;
import org.graylog.scheduler.system.SystemJobManager;
import org.graylog.security.UserContext;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.rest.models.system.SystemJobSummary;
import org.graylog2.rest.models.system.jobs.requests.TriggerRequest;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.system.jobs.LegacySystemJob;
import org.graylog2.system.jobs.LegacySystemJobFactory;
import org.graylog2.system.jobs.LegacySystemJobManager;
import org.graylog2.system.jobs.NoSuchJobException;
import org.graylog2.system.jobs.SystemJobConcurrencyException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RequiresAuthentication
@Tag(name = "System/Jobs", description = "System Jobs")
@Path("/system/jobs")
public class SystemJobResource extends RestResource {

    private static final Logger LOG = LoggerFactory.getLogger(SystemJobResource.class);

    private final LegacySystemJobFactory systemJobFactory;
    private final LegacySystemJobManager legacySystemJobManager;
    private final SystemJobManager systemJobManager;
    private final NodeId nodeId;

    private final JobResourceHandlerService jobResourceHandlerService;

    @Inject
    public SystemJobResource(LegacySystemJobFactory systemJobFactory,
                             LegacySystemJobManager legacySystemJobManager,
                             SystemJobManager systemJobManager,
                             NodeId nodeId,
                             JobResourceHandlerService jobResourceHandlerService) {
        this.systemJobFactory = systemJobFactory;
        this.legacySystemJobManager = legacySystemJobManager;
        this.systemJobManager = systemJobManager;
        this.nodeId = nodeId;
        this.jobResourceHandlerService = jobResourceHandlerService;
    }

    @GET
    @Timed
    @Operation(summary = "List currently running jobs")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<SystemJobSummary>> list() {
        final List<SystemJobSummary> jobs = new ArrayList<>();

        for (Map.Entry<String, LegacySystemJob> entry : legacySystemJobManager.getRunningJobs().entrySet()) {
            // TODO jobId is ephemeral, this is not a good key for permission checks. we should use the name of the job type (but there is no way to get it yet)
            if (isPermitted(RestPermissions.SYSTEMJOBS_READ, entry.getKey())) {
                final LegacySystemJob systemJob = entry.getValue();
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

        for (final var summary : systemJobManager.getRunningJobs(nodeId).values()) {
            if (isPermitted(RestPermissions.SYSTEMJOBS_READ, summary.jobType())) {
                jobs.add(summary);
            }
        }

        return ImmutableMap.of("jobs", jobs);
    }

    @GET
    @Timed
    @Path("/{jobId}")
    @Operation(summary = "Get information of a specific currently running job")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "Job not found.")
    })
    public SystemJobSummary get(@Parameter(name = "jobId", required = true)
                                @PathParam("jobId") @NotEmpty String jobId) {
        final LegacySystemJob systemJob = legacySystemJobManager.getRunningJobs().get(jobId);
        if (systemJob != null) {
            // TODO jobId is ephemeral, this is not a good key for permission checks. we should use the name of the job type (but there is no way to get it yet)
            checkPermission(RestPermissions.SYSTEMJOBS_READ, jobId);
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

        final Optional<SystemJobSummary> systemJobSummary = systemJobManager.getRunningJob(jobId);
        if (systemJobSummary.isPresent()) {
            checkPermission(RestPermissions.SYSTEMJOBS_READ, systemJobSummary.get().jobType());
            return systemJobSummary.get();
        }

        throw new NotFoundException("No system job with ID <" + jobId + "> found");
    }

    @POST
    @Timed
    @Operation(summary = "Trigger new job")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Job accepted.",
                         content = @Content(schema = @Schema(implementation = TriggerResponse.class))),
            @ApiResponse(responseCode = "400", description = "There is no such systemjob type."),
            @ApiResponse(responseCode = "403", description = "Maximum concurrency level of this systemjob type reached.")
    })
    @AuditEvent(type = AuditEventTypes.SYSTEM_JOB_START)
    public Response trigger(@RequestBody(required = true)
                            @Valid @NotNull TriggerRequest tr) {
        // TODO cleanup jobId vs jobName checking in permissions
        checkPermission(RestPermissions.SYSTEMJOBS_CREATE, tr.jobName());

        LegacySystemJob job;
        try {
            job = systemJobFactory.build(tr.jobName());
        } catch (NoSuchJobException e) {
            LOG.error("Such a system job type does not exist. Returning HTTP 400.");
            throw new BadRequestException(e);
        }

        try {
            legacySystemJobManager.submit(job);
        } catch (SystemJobConcurrencyException e) {
            LOG.error("Maximum concurrency level of this job reached. ", e);
            throw new ForbiddenException("Maximum concurrency level of this job reached", e);
        }

        return Response.accepted().entity(new TriggerResponse(job.getId())).build();
    }

    @DELETE
    @Timed
    @Path("/{jobId}")
    @Operation(summary = "Cancel running job")
    @Produces(MediaType.APPLICATION_JSON)
    @AuditEvent(type = AuditEventTypes.SYSTEM_JOB_STOP)
    public SystemJobSummary cancel(@Parameter(name = "jobId", required = true) @PathParam("jobId") @NotEmpty String jobId) {
        LegacySystemJob systemJob = legacySystemJobManager.getRunningJobs().get(jobId);
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
    @Operation(summary = "Acknowledge job with the given ID")
    @AuditEvent(type = AuditEventTypes.SYSTEM_JOB_ACKNOWLEDGE)
    public Response acknowledgeJob(@Context UserContext userContext,
                                   @Parameter(name = "jobId", required = true) @PathParam("jobId") @NotEmpty String jobId) {
        final int n = jobResourceHandlerService.acknowledgeJob(userContext, jobId);
        if (n < 1) {
            throw new NotFoundException("System job with ID <" + jobId + "> not found!");
        }
        return Response.accepted().build();
    }

    private record TriggerResponse(@JsonProperty("system_job_id") String systemJobId) {}
}
