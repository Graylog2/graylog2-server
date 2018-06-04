/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.sidecar.rest.resources;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableList;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.sidecar.audit.SidecarAuditEventTypes;
import org.graylog.plugins.sidecar.filter.ActiveSidecarFilter;
import org.graylog.plugins.sidecar.filter.AdministrationFilter;
import org.graylog.plugins.sidecar.filter.AdministrationFiltersFactory;
import org.graylog.plugins.sidecar.permissions.SidecarRestPermissions;
import org.graylog.plugins.sidecar.rest.models.Collector;
import org.graylog.plugins.sidecar.rest.models.CollectorAction;
import org.graylog.plugins.sidecar.rest.models.CollectorActions;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.rest.models.SidecarSummary;
import org.graylog.plugins.sidecar.rest.requests.AdministrationRequest;
import org.graylog.plugins.sidecar.rest.requests.BulkActionRequest;
import org.graylog.plugins.sidecar.rest.requests.BulkActionsRequest;
import org.graylog.plugins.sidecar.rest.responses.SidecarListResponse;
import org.graylog.plugins.sidecar.services.ActionService;
import org.graylog.plugins.sidecar.services.CollectorService;
import org.graylog.plugins.sidecar.services.ConfigurationService;
import org.graylog.plugins.sidecar.services.SidecarService;
import org.graylog.plugins.sidecar.system.SidecarConfiguration;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Api(value = "Sidecar Administration", description = "Administrate sidecars")
@Path("/sidecar/administration")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AdministrationResource extends RestResource implements PluginRestResource {
    private final SidecarService sidecarService;
    private final ConfigurationService configurationService;
    private final CollectorService collectorService;
    private final ActionService actionService;
    private final SearchQueryParser searchQueryParser;
    private final AdministrationFiltersFactory administrationFiltersFactory;
    private final ActiveSidecarFilter activeSidecarFilter;
    private final SidecarConfiguration sidecarConfiguration;

    @Inject
    public AdministrationResource(SidecarService sidecarService,
                                  ConfigurationService configurationService,
                                  CollectorService collectorService,
                                  ActionService actionService,
                                  AdministrationFiltersFactory administrationFiltersFactory,
                                  ClusterConfigService clusterConfigService) {
        this.sidecarService = sidecarService;
        this.sidecarConfiguration = clusterConfigService.getOrDefault(SidecarConfiguration.class, SidecarConfiguration.defaultConfiguration());
        this.configurationService = configurationService;
        this.collectorService = collectorService;
        this.actionService = actionService;
        this.administrationFiltersFactory = administrationFiltersFactory;
        this.activeSidecarFilter = new ActiveSidecarFilter(sidecarConfiguration.sidecarInactiveThreshold());
        this.searchQueryParser = new SearchQueryParser(Sidecar.FIELD_NODE_NAME, SidecarResource.SEARCH_FIELD_MAPPING);
    }

    @POST
    @Timed
    @ApiOperation(value = "Lists existing Sidecar registrations including compatible sidecars using pagination")
    @RequiresAuthentication
    @RequiresPermissions(SidecarRestPermissions.SIDECARS_READ)
    @NoAuditEvent("this is not changing any data")
    public SidecarListResponse administration(@ApiParam(name = "JSON body", required = true)
                                                @Valid @NotNull AdministrationRequest request) {
        final String sort = Sidecar.FIELD_NODE_NAME;
        final String order = "asc";
        final SearchQuery searchQuery = searchQueryParser.parse(request.query());

        final Optional<Predicate<Sidecar>> filters = administrationFiltersFactory.getFilters(request.filters());

        final List<Collector> collectors = getCollectors(request.filters());
        final PaginatedList<Sidecar> sidecars = sidecarService.findPaginated(searchQuery, filters.orElse(null), request.page(), request.perPage(), sort, order);
        final List<SidecarSummary> sidecarSummaries = sidecarService.toSummaryList(sidecars, activeSidecarFilter);

        final List<SidecarSummary> summariesWithCollectors = sidecarSummaries.stream()
                .map(collector -> {
                    final List<String> compatibleCollectors = collectors.stream()
                            .filter(c -> c.nodeOperatingSystem().equalsIgnoreCase(collector.nodeDetails().operatingSystem()))
                            .map(Collector::id)
                            .collect(Collectors.toList());
                    return collector.toBuilder()
                            .collectors(compatibleCollectors)
                            .build();
                })
                .filter(collectorSummary -> !filters.isPresent() || collectorSummary.collectors().size() > 0)
                .collect(Collectors.toList());

        return SidecarListResponse.create(request.query(), sidecars.pagination(), false, sort, order, summariesWithCollectors, request.filters());
    }

    @PUT
    @Timed
    @Path("/action")
    @RequiresAuthentication
    @RequiresPermissions(SidecarRestPermissions.COLLECTORS_UPDATE)
    @ApiOperation(value = "Set collector actions in bulk")
    @ApiResponses(value = {@ApiResponse(code = 400, message = "The supplied action is not valid.")})
    @AuditEvent(type = SidecarAuditEventTypes.ACTION_UPDATE)
    public Response setAction(@ApiParam(name = "JSON body", required = true)
                              @Valid @NotNull BulkActionsRequest request) {
        for (BulkActionRequest bulkActionRequest : request.collectors()) {
            final List<CollectorAction> actions = bulkActionRequest.collectorIds().stream()
                    .map(collectorId -> CollectorAction.create(collectorId, request.action()))
                    .collect(Collectors.toList());
            final CollectorActions collectorActions = actionService.fromRequest(bulkActionRequest.sidecarId(), actions);
            actionService.saveAction(collectorActions);
        }

        return Response.accepted().build();
    }


    private List<Collector> getCollectors(Map<String, String> filters) {
        final String collectorKey = AdministrationFilter.Type.COLLECTOR.toString().toLowerCase(Locale.ENGLISH);
        final String configurationKey = AdministrationFilter.Type.CONFIGURATION.toString().toLowerCase(Locale.ENGLISH);

        final List<String> collectorIds = new ArrayList<>();

        if (filters.containsKey(collectorKey)) {
            collectorIds.add(filters.get(collectorKey));
        }
        if (filters.containsKey(configurationKey)) {
            final Configuration configuration = configurationService.find(filters.get(configurationKey));
            if (!collectorIds.contains(configuration.collectorId())) {
                collectorIds.add(configuration.collectorId());
            }
        }

        switch (collectorIds.size()) {
            case 0:
                return collectorService.all();
            case 1:
                return ImmutableList.of(collectorService.find(collectorIds.get(0)));
            default:
                return new ArrayList<>();
        }
    }
}