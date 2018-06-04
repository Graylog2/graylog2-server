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

import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.sidecar.audit.SidecarAuditEventTypes;
import org.graylog.plugins.sidecar.permissions.SidecarRestPermissions;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog.plugins.sidecar.rest.models.ConfigurationSummary;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.rest.requests.ConfigurationPreviewRequest;
import org.graylog.plugins.sidecar.rest.responses.ConfigurationListResponse;
import org.graylog.plugins.sidecar.rest.responses.ConfigurationPreviewRenderResponse;
import org.graylog.plugins.sidecar.rest.responses.ValidationResponse;
import org.graylog.plugins.sidecar.services.ConfigurationService;
import org.graylog.plugins.sidecar.services.EtagService;
import org.graylog.plugins.sidecar.services.SidecarService;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

@Api(value = "Configurations", description = "Manage/Render collector configurations")
@Path("/sidecar/configurations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ConfigurationResource extends RestResource implements PluginRestResource {
    private final ConfigurationService configurationService;
    private final SidecarService sidecarService;
    private final EtagService etagService;
    private final SearchQueryParser searchQueryParser;
    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create(Configuration.FIELD_ID))
            .put("backend_id", SearchQueryField.create(Configuration.FIELD_COLLECTOR_ID))
            .put("name", SearchQueryField.create(Configuration.FIELD_NAME))
            .build();

    @Inject
    public ConfigurationResource(ConfigurationService configurationService,
                                 SidecarService sidecarService,
                                 EtagService etagService) {
        this.configurationService = configurationService;
        this.sidecarService = sidecarService;
        this.etagService = etagService;
        this.searchQueryParser = new SearchQueryParser(Configuration.FIELD_NAME, SEARCH_FIELD_MAPPING);;
    }

    @GET
    @RequiresAuthentication
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List all configurations")
    public ConfigurationListResponse listConfigurations(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                        @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                        @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                        @ApiParam(name = "sort",
                                                                         value = "The field to sort the result on",
                                                                         required = true,
                                                                         allowableValues = "name,id,backend_id")
                                                                     @DefaultValue(Configuration.FIELD_NAME) @QueryParam("sort") String sort,
                                                        @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                                                     @DefaultValue("asc") @QueryParam("order") String order) {
        final SearchQuery searchQuery = searchQueryParser.parse(query);
        final PaginatedList<Configuration> configurations = this.configurationService.findPaginated(searchQuery, page, perPage, sort, order);
        final List<ConfigurationSummary> result = configurations.stream()
                .map(ConfigurationSummary::create)
                .collect(Collectors.toList());

        return ConfigurationListResponse.create(query, configurations.pagination(), sort, order, result);
    }

    @GET
    @Path("/{id}")
    @RequiresAuthentication
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Show configuration details")
    public Configuration getConfigurations(@ApiParam(name = "id", required = true)
                                                    @PathParam("id") String id) {
        return this.configurationService.find(id);
    }

    @GET
    @Path("/validate")
    @RequiresAuthentication
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Validates configuration name")
    public ValidationResponse validateConfiguration(@ApiParam(name = "name", required = true) @QueryParam("name") String name) {
        final Configuration configuration = this.configurationService.findByName(name);
        if (configuration == null) {
            return ValidationResponse.create(false, null);
        }
        return ValidationResponse.create(true, "Configuration with name \"" + name + "\" already exists");
    }

    @GET
    @Path("/render/{sidecarId}/{configurationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_READ)
    @ApiOperation(value = "Render configuration template")
    public Response renderConfiguration(@Context HttpHeaders httpHeaders,
                                        @ApiParam(name = "sidecarId", required = true)
                                        @PathParam("sidecarId") String sidecarId,
                                        @ApiParam(name = "configurationId", required = true)
                                        @PathParam("configurationId") String configurationId) {
        String ifNoneMatch = httpHeaders.getHeaderString("If-None-Match");
        Boolean etagCached = false;
        Response.ResponseBuilder builder = Response.noContent();

        // check if client is up to date with a known valid etag
        if (ifNoneMatch != null) {
            EntityTag etag = new EntityTag(ifNoneMatch.replaceAll("\"", ""));
            if (etagService.isPresent(etag.toString())) {
                etagCached = true;
                builder = Response.notModified();
                builder.tag(etag);
            }
        }

        // fetch configuration from database if client is outdated
        if (!etagCached) {
            Sidecar sidecar = sidecarService.findByNodeId(sidecarId);
            if (sidecar == null) {
                throw new NotFoundException("Couldn't find Sidecar by ID: " + sidecarId);
            }
            Configuration configuration = configurationService.find(configurationId);
            if (configuration == null) {
                throw new NotFoundException("Couldn't find configuration by ID: " + configurationId);
            }

            Configuration collectorConfiguration = this.configurationService.renderConfigurationForCollector(sidecar, configuration);

            // add new etag to cache
            String etagString = configurationToEtag(collectorConfiguration);

            EntityTag collectorConfigurationEtag = new EntityTag(etagString);
            builder = Response.ok(collectorConfiguration);
            builder.tag(collectorConfigurationEtag);
            etagService.put(collectorConfigurationEtag.toString());

        }

        // set cache control
        CacheControl cacheControl = new CacheControl();
        cacheControl.setNoTransform(true);
        cacheControl.setPrivate(true);
        builder.cacheControl(cacheControl);

        return builder.build();
    }

    @POST
    @Path("/render/preview")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresAuthentication
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_READ)
    @ApiOperation(value = "Render preview of a configuration template")
    @NoAuditEvent("this is not changing any data")
    public ConfigurationPreviewRenderResponse renderConfiguration(@ApiParam(name = "JSON body", required = true)
                                                                  @Valid @NotNull ConfigurationPreviewRequest request) {
        String preview = this.configurationService.renderPreview(request.template());
        return ConfigurationPreviewRenderResponse.create(preview);
    }

    @POST
    @RequiresAuthentication
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_CREATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create new configuration")
    @AuditEvent(type = SidecarAuditEventTypes.CONFIGURATION_CREATE)
    public Configuration createConfiguration(@ApiParam(name = "JSON body", required = true)
                                             @Valid @NotNull Configuration request) {
        Configuration configuration = configurationService.fromRequest(request);
        return configurationService.save(configuration);
    }

    @POST
    @Path("/{id}/{name}")
    @RequiresAuthentication
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_CREATE)
    @ApiOperation(value = "Copy a configuration")
    @AuditEvent(type = SidecarAuditEventTypes.CONFIGURATION_CLONE)
    public Response copyConfiguration(@ApiParam(name = "id", required = true)
                                      @PathParam("id") String id,
                                      @PathParam("name") String name) throws NotFoundException {
        final Configuration configuration = configurationService.copyConfiguration(id, name);
        configurationService.save(configuration);
        return Response.accepted().build();
    }

    @PUT
    @Path("/{id}")
    @RequiresAuthentication
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_UPDATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a configuration")
    @AuditEvent(type = SidecarAuditEventTypes.CONFIGURATION_UPDATE)
    public Configuration updateConfiguration(@ApiParam(name = "id", required = true)
                                             @PathParam("id") String id,
                                             @ApiParam(name = "JSON body", required = true)
                                             @Valid @NotNull Configuration request) {
        etagService.invalidateAll();
        Configuration configuration = configurationService.fromRequest(id, request);
        return configurationService.save(configuration);
    }

    @DELETE
    @Path("/{id}")
    @RequiresAuthentication
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_UPDATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete a configuration")
    @AuditEvent(type = SidecarAuditEventTypes.CONFIGURATION_DELETE)
    public Response deleteConfiguration(@ApiParam(name = "id", required = true)
                                        @PathParam("id") String id) {
        int deleted = configurationService.delete(id);
        if (deleted == 0) {
            return Response.notModified().build();
        }
        etagService.invalidateAll();
        return Response.accepted().build();
    }

    private String configurationToEtag(Configuration configuration) {
        return Hashing.md5()
                .hashInt(configuration.hashCode())  // avoid negative values
                .toString();
    }
}
