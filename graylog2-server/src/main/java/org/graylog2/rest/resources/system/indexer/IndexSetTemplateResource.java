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
package org.graylog2.rest.resources.system.indexer;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.indexer.indexset.template.IndexSetTemplate;
import org.graylog2.indexer.indexset.template.IndexSetTemplateData;
import org.graylog2.indexer.indexset.template.IndexSetTemplateService;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.List;

import static org.graylog2.audit.AuditEventTypes.INDEX_SET_TEMPLATE_CREATE;
import static org.graylog2.audit.AuditEventTypes.INDEX_SET_TEMPLATE_DELETE;
import static org.graylog2.audit.AuditEventTypes.INDEX_SET_TEMPLATE_UPDATE;
import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;
import static org.graylog2.shared.utilities.StringUtils.f;

@RequiresAuthentication
@Api(value = "System/IndexSets/Templates", description = "Index-set Configuration Template Management", tags = {CLOUD_VISIBLE})
@Path("/system/indices/index_sets/templates")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IndexSetTemplateResource extends RestResource {

    private final IndexSetTemplateService templateService;

    @Inject
    public IndexSetTemplateResource(final IndexSetTemplateService templateService) {
        this.templateService = templateService;
    }

    @GET
    @Path("/{template_id}")
    @Timed
    @NoAuditEvent("No change to the DB")
    @ApiOperation(value = "Gets template by id")
    public IndexSetTemplate retrieveById(@ApiParam(name = "template_id") @PathParam("template_id") String templateId) {
        checkPermission(RestPermissions.INDEX_SET_TEMPLATES_READ, templateId);
        return templateService.get(templateId)
                .orElseThrow(() -> new NotFoundException("No template with id : " + templateId));
    }

    @GET
    @Path("/paginated")
    @Timed
    @NoAuditEvent("No change to the DB")
    @ApiOperation(value = "Gets template by id")
    public PageListResponse<IndexSetTemplate> getPage(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                      @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                      @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                      @ApiParam(name = "filters") @QueryParam("filters") List<String> filters,
                                                      @ApiParam(name = "sort",
                                                                value = "The field to sort the result on",
                                                                required = true,
                                                                allowableValues = "name")
                                                          @DefaultValue(IndexSetTemplate.TITLE_FIELD_NAME) @QueryParam("sort") String sort,
                                                      @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                                      @DefaultValue("asc") @QueryParam("order") String order) {
        checkPermission(RestPermissions.INDEX_SET_TEMPLATES_READ);
        return templateService.getPaginated(query, filters, page, perPage, sort, order);
    }

    @POST
    @Timed
    @AuditEvent(type = INDEX_SET_TEMPLATE_CREATE)
    @ApiOperation(value = "Creates a new editable template")
    public IndexSetTemplate create(@ApiParam(name = "templateData") IndexSetTemplateData templateData) {
        checkPermission(RestPermissions.INDEX_SET_TEMPLATES_CREATE);

        // Templates created via the API are always writable
        return templateService.save(new IndexSetTemplate(templateData, false));
    }

    @PUT
    @Timed
    @AuditEvent(type = INDEX_SET_TEMPLATE_UPDATE)
    @ApiOperation(value = "Updates existing template")
    public void update(@ApiParam(name = "template") IndexSetTemplate template) throws IllegalAccessException {
        checkPermission(RestPermissions.INDEX_SET_TEMPLATES_EDIT, template.id());
        checkReadOnly(template.id());
        final boolean updated = templateService.update(template.id(), template);
        if (!updated) {
            throw new NotFoundException(f("Template %s does not exist", template.id()));
        }
    }

    @DELETE
    @Path("/{template_id}")
    @Timed
    @AuditEvent(type = INDEX_SET_TEMPLATE_DELETE)
    @ApiOperation(value = "Removes a template")
    public void delete(@ApiParam(name = "template_id") @PathParam("template_id") String templateId) throws IllegalAccessException {
        checkPermission(RestPermissions.INDEX_SET_TEMPLATES_DELETE, templateId);
        checkReadOnly(templateId);
        templateService.delete(templateId);
    }

    private IndexSetTemplate checkReadOnly(String templateId) throws IllegalAccessException {
        final IndexSetTemplate template = templateService.get(templateId)
                .orElseThrow(() -> new NotFoundException("No template with id : " + templateId));
        if (template.isReadOnly()) {
            throw new IllegalAccessException(f("Template %s is read-only and cannot be modified or deleted", template.name()));
        }
        return template;
    }
}
