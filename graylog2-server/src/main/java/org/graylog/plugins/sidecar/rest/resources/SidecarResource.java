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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.base.Function;
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
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.sidecar.audit.SidecarAuditEventTypes;
import org.graylog.plugins.sidecar.common.SidecarPluginConfiguration;
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
import org.graylog.plugins.sidecar.services.EtagService;
import org.graylog.plugins.sidecar.services.SidecarService;
import org.graylog.plugins.sidecar.system.SidecarConfiguration;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.users.responses.BasicUserResponse;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.PublicCloudAPI;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.users.UserManagementService;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static org.graylog2.shared.security.RestPermissions.USERS_READ;

@PublicCloudAPI
@Tag(name = "Sidecar", description = "Manage Sidecar fleet")
@Path("/sidecars")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class SidecarResource extends RestResource implements PluginRestResource {
    protected static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create("_id", SearchQueryField.Type.OBJECT_ID))
            .put("node_id", SearchQueryField.create(Sidecar.FIELD_NODE_ID))
            .put("name", SearchQueryField.create(Sidecar.FIELD_NODE_NAME))
            .put("sidecar_version", SearchQueryField.create(Sidecar.FIELD_SIDECAR_VERSION))
            .put("last_seen", SearchQueryField.create(Sidecar.FIELD_LAST_SEEN, SearchQueryField.Type.DATE))
            .put("operating_system", SearchQueryField.create(Sidecar.FIELD_OPERATING_SYSTEM))
            .put("status", SearchQueryField.create(Sidecar.FIELD_STATUS, SearchQueryField.Type.INT))
            .build();

    private final SidecarService sidecarService;
    private final ActionService actionService;
    private final EtagService etagService;
    private final ActiveSidecarFilter activeSidecarFilter;
    private final SearchQueryParser searchQueryParser;
    private final SidecarStatusMapper sidecarStatusMapper;
    private final SidecarConfiguration sidecarConfiguration;
    private final UserManagementService userManagementService;
    private final String sidecarUserName;

    @Inject
    public SidecarResource(SidecarService sidecarService,
                           ActionService actionService,
                           ClusterConfigService clusterConfigService,
                           SidecarStatusMapper sidecarStatusMapper,
                           EtagService etagService,
                           UserManagementService userManagementService,
                           SidecarPluginConfiguration sidecarPluginConfiguration) {
        this.sidecarService = sidecarService;
        this.sidecarConfiguration = clusterConfigService.getOrDefault(SidecarConfiguration.class, SidecarConfiguration.defaultConfiguration());
        this.actionService = actionService;
        this.userManagementService = userManagementService;
        this.sidecarUserName = sidecarPluginConfiguration.getUser();
        this.activeSidecarFilter = new ActiveSidecarFilter(sidecarConfiguration.sidecarInactiveThreshold());
        this.sidecarStatusMapper = sidecarStatusMapper;
        this.etagService = etagService;
        this.searchQueryParser = new SearchQueryParser(Sidecar.FIELD_NODE_NAME, SEARCH_FIELD_MAPPING);
    }

    @GET
    @Timed
    @Path("/all")
    @Operation(summary = "Lists all existing Sidecar registrations")
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
    @Operation(summary = "Lists existing Sidecar registrations using pagination")
    @RequiresPermissions(SidecarRestPermissions.SIDECARS_READ)
    public SidecarListResponse sidecars(@Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                        @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                        @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                        @Parameter(name = "sort",
                                                  description = "The field to sort the result on",
                                                  required = true,
                                                  schema = @Schema(allowableValues = {"title", "description", "name", "id"}))
                                        @DefaultValue(Sidecar.FIELD_NODE_NAME) @QueryParam("sort") String sort,
                                        @Parameter(name = "order", description = "The sort direction", schema = @Schema(allowableValues = {"asc", "desc"}))
                                        @DefaultValue("asc") @QueryParam("order") SortOrder order,
                                        @Parameter(name = "only_active") @QueryParam("only_active") @DefaultValue("false") boolean onlyActive) {
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
    @Operation(summary = "Returns at most one Sidecar summary for the specified id")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "No Sidecar with the specified id exists")
    })
    @RequiresPermissions(SidecarRestPermissions.SIDECARS_READ)
    public SidecarSummary get(@Parameter(name = "sidecarId", required = true)
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
    @Operation(summary = "Create/update a Sidecar registration",
                  description = "This is a stateless method which upserts a Sidecar registration")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Returns registration response",
                    content = @Content(schema = @Schema(implementation = RegistrationResponse.class))),
            @ApiResponse(responseCode = "400", description = "The supplied request is not valid.")
    })
    @RequiresPermissions(SidecarRestPermissions.SIDECARS_UPDATE)
    @NoAuditEvent("this is only a ping from Sidecars, and would overflow the audit log")
    public Response register(@Parameter(name = "sidecarId", description = "The id this Sidecar is registering as.", required = true)
                             @PathParam("sidecarId") @NotEmpty String nodeId,
                             @RequestBody(required = true)
                             @Valid @NotNull RegistrationRequest request,
                             @HeaderParam(value = "If-None-Match") String ifNoneMatch,
                             @HeaderParam(value = "X-Graylog-Sidecar-Version") @NotEmpty String sidecarVersion) throws JsonProcessingException {

        Sidecar sidecar;
        final Sidecar oldSidecar = sidecarService.findByNodeId(nodeId);
        if (oldSidecar != null) {
            sidecar = oldSidecar.toBuilder()
                    .nodeName(request.nodeName())
                    .nodeDetails(request.nodeDetails())
                    .sidecarVersion(sidecarVersion)
                    .lastSeen(DateTime.now(DateTimeZone.UTC))
                    .build();
        } else {
            sidecar = sidecarService.fromRequest(nodeId, request, sidecarVersion);
        }

        // If the sidecar has the recent registration, return with HTTP 304
        if (ifNoneMatch != null) {
            EntityTag etag = new EntityTag(ifNoneMatch.replaceAll("\"", ""));
            if (etagService.registrationIsCached(sidecar.nodeId(), etag.toString())) {
                sidecarService.save(sidecar);
                return Response.notModified().tag(etag).build();
            }
        }

        final Sidecar updated = sidecarService.updateTaggedConfigurationAssignments(sidecar);
        sidecarService.save(updated);
        sidecar = updated;

        final CollectorActions collectorActions = actionService.findActionBySidecar(nodeId, true);
        List<CollectorAction> collectorAction = null;
        if (collectorActions != null) {
            collectorAction = collectorActions.action();
        }
        RegistrationResponse sidecarRegistrationResponse = RegistrationResponse.create(
                SidecarRegistrationConfiguration.create(
                        sidecarConfiguration.sidecarUpdateInterval().toStandardDuration().getStandardSeconds(),
                        sidecarConfiguration.sidecarSendStatus()),
                sidecarConfiguration.sidecarConfigurationOverride(),
                collectorAction,
                sidecar.assignments());
        // add new etag to cache
        EntityTag registrationEtag = etagService.buildEntityTagForResponse(sidecarRegistrationResponse);
        etagService.addSidecarRegistration(sidecar.nodeId(), registrationEtag.toString());

        return Response.accepted(sidecarRegistrationResponse).tag(registrationEtag).build();
    }

    @PUT
    @Timed
    @Path("/configurations")
    @Operation(summary = "Assign configurations to sidecars")
    @RequiresPermissions({SidecarRestPermissions.SIDECARS_READ, SidecarRestPermissions.SIDECARS_UPDATE})
    @AuditEvent(type = SidecarAuditEventTypes.SIDECAR_UPDATE)
    public Response assignConfiguration(@RequestBody(required = true)
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
                Sidecar sidecar = sidecarService.applyManualAssignments(nodeId, nodeRelations);
                sidecarService.save(sidecar);
                etagService.invalidateRegistration(sidecar.nodeId());
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

    @GET
    @Path("/user")
    @Operation(summary = "Get basic sidecar user")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Success", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "The sidecar user could not be found.")
    })
    public BasicUserResponse getBasicSidecarUser() {

        if (!isPermitted(USERS_READ, sidecarUserName)) {
            throw new ForbiddenException("Not allowed to view user " + sidecarUserName);
        }

        final User user = userManagementService.load(sidecarUserName);
        if (user == null) {
            throw new NotFoundException("Couldn't find user " + sidecarUserName);
        }
        return BasicUserResponse.builder()
                .id(user.getId())
                .username(user.getName())
                .fullName(user.getFullName())
                .readOnly(user.isReadOnly())
                .isServiceAccount(user.isServiceAccount())
                .build();
    }
}
