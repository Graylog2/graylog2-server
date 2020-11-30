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
import com.google.common.collect.ImmutableMap;
import com.google.common.hash.Hashing;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.plugins.sidecar.audit.SidecarAuditEventTypes;
import org.graylog.plugins.sidecar.permissions.SidecarRestPermissions;
import org.graylog.plugins.sidecar.rest.models.CollectorUpload;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog.plugins.sidecar.rest.models.ConfigurationSummary;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.rest.requests.ConfigurationAssignment;
import org.graylog.plugins.sidecar.rest.requests.ConfigurationPreviewRequest;
import org.graylog.plugins.sidecar.rest.responses.CollectorUploadListResponse;
import org.graylog.plugins.sidecar.rest.responses.ConfigurationListResponse;
import org.graylog.plugins.sidecar.rest.responses.ConfigurationPreviewRenderResponse;
import org.graylog.plugins.sidecar.rest.responses.ConfigurationSidecarsResponse;
import org.graylog.plugins.sidecar.services.ConfigurationService;
import org.graylog.plugins.sidecar.services.EtagService;
import org.graylog.plugins.sidecar.services.ImportService;
import org.graylog.plugins.sidecar.services.SidecarService;
import org.graylog.plugins.sidecar.template.RenderTemplateException;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.plugin.rest.ValidationResult;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.MoreObjects.firstNonNull;

@Api(value = "Sidecar/Configurations", description = "Manage/Render collector configurations")
@Path("/sidecar/configurations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class ConfigurationResource extends RestResource implements PluginRestResource {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationResource.class);

    // a file is created by the Sidecar based on the configuration name so we basically check for invalid paths here
    private static final Pattern VALID_NAME_PATTERN = Pattern.compile("^[^;*?\"<>|&]+$");

    private final ConfigurationService configurationService;
    private final SidecarService sidecarService;
    private final EtagService etagService;
    private final ImportService importService;
    private final SearchQueryParser searchQueryParser;
    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put("id", SearchQueryField.create(Configuration.FIELD_ID))
            .put("collector_id", SearchQueryField.create(Configuration.FIELD_COLLECTOR_ID))
            .put("name", SearchQueryField.create(Configuration.FIELD_NAME))
            .build();

    @Inject
    public ConfigurationResource(ConfigurationService configurationService,
                                 SidecarService sidecarService,
                                 EtagService etagService,
                                 ImportService importService) {
        this.configurationService = configurationService;
        this.sidecarService = sidecarService;
        this.etagService = etagService;
        this.importService = importService;
        this.searchQueryParser = new SearchQueryParser(Configuration.FIELD_NAME, SEARCH_FIELD_MAPPING);;
    }

    @GET
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List all configurations")
    public ConfigurationListResponse listConfigurations(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                        @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                        @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                        @ApiParam(name = "sort",
                                                                         value = "The field to sort the result on",
                                                                         required = true,
                                                                         allowableValues = "name,id,collector_id")
                                                                     @DefaultValue(Configuration.FIELD_NAME) @QueryParam("sort") String sort,
                                                        @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                                                     @DefaultValue("asc") @QueryParam("order") String order) {
        final SearchQuery searchQuery = searchQueryParser.parse(query);
        final PaginatedList<Configuration> configurations = this.configurationService.findPaginated(searchQuery, page, perPage, sort, order);
        final long total = this.configurationService.count();
        final List<ConfigurationSummary> result = configurations.stream()
                .map(ConfigurationSummary::create)
                .collect(Collectors.toList());

        return ConfigurationListResponse.create(query, configurations.pagination(), total, sort, order, result);
    }

    @GET
    @Path("/uploads")
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "List all uploaded configurations")
    public CollectorUploadListResponse listImports(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page) {
        // sort by creation date, latest on top of the list
        final PaginatedList<CollectorUpload> uploads = this.importService.findPaginated(page, 10, "created", "desc");
        final long total = this.importService.count();
        final List<CollectorUpload> result = new ArrayList<>(uploads);

        return CollectorUploadListResponse.create(uploads.pagination(), total, result);
    }

    @GET
    @Path("/{id}")
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Show configuration details")
    public Configuration getConfigurations(@ApiParam(name = "id", required = true)
                                           @PathParam("id") String id) {
        final Configuration configuration = this.configurationService.find(id);
        if (configuration == null) {
            throw new NotFoundException("Could not find Configuration <" + id + ">.");
        }
        return configuration;
    }

    @GET
    @Path("/{id}/sidecars")
    @RequiresPermissions({SidecarRestPermissions.CONFIGURATIONS_READ, SidecarRestPermissions.SIDECARS_READ})
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Show sidecars using the given configuration")
    public ConfigurationSidecarsResponse getConfigurationSidecars(@ApiParam(name = "id", required = true)
                                                                      @PathParam("id") String id) {
        final Configuration configuration = this.configurationService.find(id);
        if (configuration == null) {
            throw new NotFoundException("Could not find Configuration <" + id + ">.");
        }
        final List<String> sidecarsWithConfiguration = sidecarService.all().stream()
                .filter(sidecar -> isConfigurationAssignedToSidecar(configuration.id(), sidecar))
                .map(Sidecar::id)
                .collect(Collectors.toList());
        return ConfigurationSidecarsResponse.create(configuration.id(), sidecarsWithConfiguration);
    }

    @POST
    @Path("/validate")
    @NoAuditEvent("Validation only")
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_READ)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Validates configuration parameters")
    public ValidationResult validateConfiguration(@Valid @ApiParam("configuration") Configuration toValidate) {
        return validate(toValidate);
    }

    @GET
    @Timed
    @Path("/render/{sidecarId}/{configurationId}")
    @Produces(MediaType.APPLICATION_JSON)
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_READ)
    @ApiOperation(value = "Render configuration template")
    public Response renderConfiguration(@Context HttpHeaders httpHeaders,
                                        @ApiParam(name = "sidecarId", required = true)
                                        @PathParam("sidecarId") String sidecarId,
                                        @ApiParam(name = "configurationId", required = true)
                                        @PathParam("configurationId") String configurationId) throws RenderTemplateException {
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
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_READ)
    @ApiOperation(value = "Render preview of a configuration template")
    @NoAuditEvent("this is not changing any data")
    public ConfigurationPreviewRenderResponse renderConfiguration(@ApiParam(name = "JSON body", required = true)
                                                                  @Valid @NotNull ConfigurationPreviewRequest request) {
        try {
            String preview = this.configurationService.renderPreview(request.template());
            return ConfigurationPreviewRenderResponse.create(preview);
        } catch (RenderTemplateException e) {
            throw new BadRequestException("Could not render template preview: " + e.getMessage());
        }
    }

    @POST
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_CREATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Create new configuration")
    @AuditEvent(type = SidecarAuditEventTypes.CONFIGURATION_CREATE)
    public Response createConfiguration(@ApiParam(name = "JSON body", required = true)
                                             @Valid @NotNull Configuration request) {
        final Configuration configuration = configurationFromRequest(null, request);
        final ValidationResult validationResult = validate(configuration);
        if (validationResult.failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(validationResult).build();
        }

        return Response.ok().entity(configurationService.save(configuration)).build();
    }

    @POST
    @Path("/{id}/{name}")
    @RequiresPermissions({SidecarRestPermissions.CONFIGURATIONS_READ, SidecarRestPermissions.CONFIGURATIONS_CREATE})
    @ApiOperation(value = "Copy a configuration")
    @AuditEvent(type = SidecarAuditEventTypes.CONFIGURATION_CLONE)
    public Response copyConfiguration(@ApiParam(name = "id", required = true)
                                      @PathParam("id") String id,
                                      @PathParam("name") String name) throws NotFoundException {
        final Configuration configuration = configurationService.copyConfiguration(id, name);
        final ValidationResult validationResult = validate(configuration);
        if (validationResult.failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(validationResult).build();
        }
        configurationService.save(configuration);
        return Response.accepted().build();
    }

    @PUT
    @Path("/{id}")
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_UPDATE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Update a configuration")
    @AuditEvent(type = SidecarAuditEventTypes.CONFIGURATION_UPDATE)
    public Response updateConfiguration(@ApiParam(name = "id", required = true)
                                             @PathParam("id") String id,
                                             @ApiParam(name = "JSON body", required = true)
                                             @Valid @NotNull Configuration request) {
        final Configuration previousConfiguration = configurationService.find(id);
        if (previousConfiguration == null) {
            throw new NotFoundException("Could not find Configuration <" + id + ">.");
        }

        // Only allow changing the associated collector ID if the configuration is not in use
        if (!previousConfiguration.collectorId().equals(request.collectorId())) {
            if (isConfigurationInUse(id)) {
                throw new BadRequestException("Configuration still in use, cannot change collector type.");
            }
        }

        final Configuration updatedConfiguration = configurationFromRequest(id, request);
        final ValidationResult validationResult = validate(updatedConfiguration);
        if (validationResult.failed()) {
            return Response.status(Response.Status.BAD_REQUEST).entity(validationResult).build();
        }
        etagService.invalidateAll();

        return Response.ok().entity(configurationService.save(updatedConfiguration)).build();
    }

    @DELETE
    @Path("/{id}")
    @RequiresPermissions(SidecarRestPermissions.CONFIGURATIONS_DELETE)
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Delete a configuration")
    @AuditEvent(type = SidecarAuditEventTypes.CONFIGURATION_DELETE)
    public Response deleteConfiguration(@ApiParam(name = "id", required = true)
                                        @PathParam("id") String id) {
        if (isConfigurationInUse(id)) {
            throw new BadRequestException("Configuration still in use, cannot delete.");
        }

        int deleted = configurationService.delete(id);
        if (deleted == 0) {
            return Response.notModified().build();
        }
        etagService.invalidateAll();
        return Response.accepted().build();
    }

    private ValidationResult validate(Configuration toValidate) {
        final Optional<Configuration> configurationOptional;
        final Configuration configuration;
        final ValidationResult validation = new ValidationResult();

        configurationOptional = Optional.ofNullable(configurationService.findByName(toValidate.name()));
        if (configurationOptional.isPresent()) {
            configuration = configurationOptional.get();
            if (!configuration.id().equals(toValidate.id())) {
                // a configuration exists with a different id, so the name is already in use, fail validation
                validation.addError("name", "Configuration \"" + toValidate.name() + "\" already exists");
            }
        }

        if (toValidate.name().isEmpty()) {
            validation.addError("name", "Configuration name cannot be empty.");
        } else if (!VALID_NAME_PATTERN.matcher(toValidate.name()).matches()) {
                validation.addError("name", "Configuration name can not include the following characters: ; * ? \" < > | &");
        }

        if (toValidate.collectorId().isEmpty()) {
            validation.addError("collector_id", "Associated collector ID cannot be empty.");
        }

        if (toValidate.color().isEmpty()) {
            validation.addError("color", "Collector color cannot be empty.");
        }

        if (toValidate.template().isEmpty()) {
            validation.addError("template", "Collector template cannot be empty.");
        }

        try {
            this.configurationService.renderPreview(toValidate.template());
        } catch (RenderTemplateException e) {
            validation.addError("template", "Template error: " + e.getMessage());
        }

        return validation;
    }

    private boolean isConfigurationInUse(String configurationId) {
        return sidecarService.all().stream().anyMatch(sidecar -> isConfigurationAssignedToSidecar(configurationId, sidecar));
    }

    private boolean isConfigurationAssignedToSidecar(String configurationId, Sidecar sidecar) {
        final List<ConfigurationAssignment> assignments = firstNonNull(sidecar.assignments(), new ArrayList<>());
        return assignments.stream().anyMatch(assignment -> assignment.configurationId().equals(configurationId));
    }

    private String configurationToEtag(Configuration configuration) {
        return Hashing.md5()
                .hashInt(configuration.hashCode())  // avoid negative values
                .toString();
    }

    private Configuration configurationFromRequest(String id, Configuration request) {
        Configuration configuration;
        if (id == null) {
            configuration = configurationService.fromRequest(request);
        } else {
            configuration = configurationService.fromRequest(id, request);
        }
        return configuration;
    }
}
