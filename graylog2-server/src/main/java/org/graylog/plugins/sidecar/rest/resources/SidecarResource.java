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
package org.graylog.plugins.sidecar.rest.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.base.Function;
import com.google.common.collect.ImmutableMap;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.sidecar.audit.SidecarAuditEventTypes;
import org.graylog.plugins.sidecar.filter.ActiveSidecarFilter;
import org.graylog.plugins.sidecar.mapper.SidecarStatusMapper;
import org.graylog.plugins.sidecar.permissions.SidecarRestPermissions;
import org.graylog.plugins.sidecar.rest.models.CollectorAction;
import org.graylog.plugins.sidecar.rest.models.CollectorActions;
import org.graylog.plugins.sidecar.rest.models.NodeConfiguration;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.rest.models.SidecarRegistrationConfiguration;
import org.graylog.plugins.sidecar.rest.models.SidecarSummary;
import org.graylog.plugins.sidecar.rest.requests.ConfigurationAssignment;
import org.graylog.plugins.sidecar.rest.requests.NodeConfigurationRequest;
import org.graylog.plugins.sidecar.rest.requests.RegistrationRequest;
import org.graylog.plugins.sidecar.rest.responses.RegistrationResponse;
import org.graylog.plugins.sidecar.rest.responses.SidecarListResponse;
import org.graylog.plugins.sidecar.services.ActionService;
import org.graylog.plugins.sidecar.services.SidecarService;
import org.graylog.plugins.sidecar.system.SidecarConfiguration;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.hibernate.validator.constraints.NotEmpty;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Api(value = "Sidecar", description = "Manage Sidecar fleet")
@Path("/sidecars")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class SidecarResource extends RestResource implements PluginRestResource {
    protected static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create(Sidecar.FIELD_ID))
            .put("node_id", SearchQueryField.create(Sidecar.FIELD_NODE_ID))
            .put("name", SearchQueryField.create(Sidecar.FIELD_NODE_NAME))
            .put("sidecar_version", SearchQueryField.create(Sidecar.FIELD_SIDECAR_VERSION))
            .put("last_seen", SearchQueryField.create(Sidecar.FIELD_LAST_SEEN, SearchQueryField.Type.DATE))
            .put("operating_system", SearchQueryField.create(Sidecar.FIELD_OPERATING_SYSTEM))
            .put("status", SearchQueryField.create(Sidecar.FIELD_STATUS, SearchQueryField.Type.INT))
            .build();

    private final SidecarService sidecarService;
    private final ActionService actionService;
    private final ActiveSidecarFilter activeSidecarFilter;
    private final SearchQueryParser searchQueryParser;
    private final SidecarStatusMapper sidecarStatusMapper;
    private final SidecarConfiguration sidecarConfiguration;

    @Inject
    public SidecarResource(SidecarService sidecarService,
                           ActionService actionService,
                           ClusterConfigService clusterConfigService,
                           SidecarStatusMapper sidecarStatusMapper) {
        this.sidecarService = sidecarService;
        this.sidecarConfiguration = clusterConfigService.getOrDefault(SidecarConfiguration.class, SidecarConfiguration.defaultConfiguration());
        this.actionService = actionService;
        this.activeSidecarFilter = new ActiveSidecarFilter(sidecarConfiguration.sidecarInactiveThreshold());
        this.sidecarStatusMapper = sidecarStatusMapper;
        this.searchQueryParser = new SearchQueryParser(Sidecar.FIELD_NODE_NAME, SEARCH_FIELD_MAPPING);
    }

    @GET
    @Timed
    @Path("/all")
    @ApiOperation(value = "Lists all existing Sidecar registrations")
    @RequiresPermissions(SidecarRestPermissions.SIDECARS_READ)
    public SidecarListResponse all() {
        final List<Sidecar> sidecars = sidecarService.all();
        final List<SidecarSummary> sidecarSummaries = sidecarService.toSummaryList(sidecars, activeSidecarFilter);
        return SidecarListResponse.create("",
                PaginatedList.PaginationInfo.create(sidecarSummaries.size(),
                        sidecarSummaries.size(),
                        1,
                        sidecarSummaries.size()),
                sidecarSummaries.size(),
                false,
                null,
                null,
                sidecarSummaries);
    }

    @GET
    @Timed
    @ApiOperation(value = "Lists existing Sidecar registrations using pagination")
    @RequiresPermissions(SidecarRestPermissions.SIDECARS_READ)
    public SidecarListResponse sidecars(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                        @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                        @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                        @ApiParam(name = "sort",
                                                value = "The field to sort the result on",
                                                required = true,
                                                allowableValues = "title,description,name,id")
                                        @DefaultValue(Sidecar.FIELD_NODE_NAME) @QueryParam("sort") String sort,
                                        @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                        @DefaultValue("asc") @QueryParam("order") String order,
                                        @ApiParam(name = "only_active") @QueryParam("only_active") @DefaultValue("false") boolean onlyActive) {
        final String mappedQuery = sidecarStatusMapper.replaceStringStatusSearchQuery(query);
        SearchQuery searchQuery;
        try {
            searchQuery = searchQueryParser.parse(mappedQuery);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }
        final PaginatedList<Sidecar> sidecars = onlyActive ?
                sidecarService.findPaginated(searchQuery, activeSidecarFilter, page, perPage, sort, order) :
                sidecarService.findPaginated(searchQuery, page, perPage, sort, order);
        final List<SidecarSummary> collectorSummaries = sidecarService.toSummaryList(sidecars, activeSidecarFilter);
        final long total = sidecarService.count();
        return SidecarListResponse.create(query, sidecars.pagination(), total, onlyActive, sort, order, collectorSummaries);
    }

    @GET
    @Timed
    @Path("/{sidecarId}")
    @ApiOperation(value = "Returns at most one Sidecar summary for the specified id")
    @ApiResponses(value = {
            @ApiResponse(code = 404, message = "No Sidecar with the specified id exists")
    })
    @RequiresPermissions(SidecarRestPermissions.SIDECARS_READ)
    public SidecarSummary get(@ApiParam(name = "sidecarId", required = true)
                              @PathParam("sidecarId") @NotEmpty String sidecarId) {
        final Sidecar sidecar = sidecarService.findByNodeId(sidecarId);
        if (sidecar == null) {
            throw new NotFoundException("Could not find sidecar <" + sidecarId + ">");
        }
        return sidecar.toSummary(activeSidecarFilter);
    }

    @PUT
    @Timed
    @Path("/{sidecarId}")
    @ApiOperation(value = "Create/update a Sidecar registration",
            notes = "This is a stateless method which upserts a Sidecar registration")
    @ApiResponses(value = {
            @ApiResponse(code = 400, message = "The supplied request is not valid.")
    })
    @RequiresPermissions(SidecarRestPermissions.SIDECARS_UPDATE)
    @NoAuditEvent("this is only a ping from Sidecars, and would overflow the audit log")
    public Response register(@ApiParam(name = "sidecarId", value = "The id this Sidecar is registering as.", required = true)
                             @PathParam("sidecarId") @NotEmpty String sidecarId,
                             @ApiParam(name = "JSON body", required = true)
                             @Valid @NotNull RegistrationRequest request,
                             @HeaderParam(value = "X-Graylog-Sidecar-Version") @NotEmpty String sidecarVersion) {
        final Sidecar newSidecar;
        final Sidecar oldSidecar = sidecarService.findByNodeId(sidecarId);
        List<ConfigurationAssignment> assignments = null;
        if (oldSidecar != null) {
            assignments = oldSidecar.assignments();
            newSidecar = oldSidecar.toBuilder()
                    .nodeName(request.nodeName())
                    .nodeDetails(request.nodeDetails())
                    .sidecarVersion(sidecarVersion)
                    .lastSeen(DateTime.now(DateTimeZone.UTC))
                    .build();
        } else {
            newSidecar = sidecarService.fromRequest(sidecarId, request, sidecarVersion);
        }
        sidecarService.save(newSidecar);

        final CollectorActions collectorActions = actionService.findActionBySidecar(sidecarId, true);
        List<CollectorAction> collectorAction = null;
        if (collectorActions != null) {
            collectorAction = collectorActions.action();
        }
        RegistrationResponse sidecarRegistrationResponse = RegistrationResponse.create(
                SidecarRegistrationConfiguration.create(
                        this.sidecarConfiguration.sidecarUpdateInterval().toStandardDuration().getStandardSeconds(),
                        this.sidecarConfiguration.sidecarSendStatus()),
                this.sidecarConfiguration.sidecarConfigurationOverride(),
                collectorAction,
                assignments);
        return Response.accepted(sidecarRegistrationResponse).build();
    }

    @PUT
    @Timed
    @Path("/configurations")
    @ApiOperation(value = "Assign configurations to sidecars")
    @RequiresPermissions(SidecarRestPermissions.SIDECARS_UPDATE)
    @AuditEvent(type = SidecarAuditEventTypes.SIDECAR_UPDATE)
    public Response assignConfiguration(@ApiParam(name = "JSON body", required = true)
                                        @Valid @NotNull NodeConfigurationRequest request) throws NotFoundException {
        List<String> nodeIdList = request.nodes().stream()
                .filter(distinctByKey(NodeConfiguration::nodeId))
                .map(NodeConfiguration::nodeId)
                .collect(Collectors.toList());

        for (String nodeId : nodeIdList) {
            List<ConfigurationAssignment> nodeRelations = request.nodes().stream()
                    .filter(a -> a.nodeId().equals(nodeId))
                    .flatMap(a -> a.assignments().stream())
                    .collect(Collectors.toList());
            try {
                Sidecar sidecar = sidecarService.assignConfiguration(nodeId, nodeRelations);
                sidecarService.save(sidecar);
            } catch (org.graylog2.database.NotFoundException e) {
                throw new NotFoundException(e.getMessage());
            }
        }

        return Response.accepted().build();
    }

    private static <T> Predicate<T> distinctByKey(Function<? super T, Object> keyExtractor) {
        Map<Object, Boolean> map = new ConcurrentHashMap<>();
        return t -> map.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}
