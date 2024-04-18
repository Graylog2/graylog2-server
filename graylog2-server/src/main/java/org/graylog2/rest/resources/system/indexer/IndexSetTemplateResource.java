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
import jakarta.validation.Validator;
import jakarta.ws.rs.BadRequestException;
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
import org.graylog2.indexer.IndexSetValidator;
import org.graylog2.indexer.indexset.IndexSetConfig;
import org.graylog2.indexer.indexset.template.IndexSetDefaultTemplateService;
import org.graylog2.indexer.indexset.template.IndexSetTemplate;
import org.graylog2.indexer.indexset.template.IndexSetTemplateConfig;
import org.graylog2.indexer.indexset.template.IndexSetTemplateData;
import org.graylog2.indexer.indexset.template.IndexSetTemplateService;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.joda.time.Duration;

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
    private final IndexSetValidator indexSetValidator;
    private final Validator validator;
    private final IndexSetTemplateService templateService;

    private final IndexSetDefaultTemplateService indexSetDefaultTemplateService;

    @Inject
    public IndexSetTemplateResource(IndexSetValidator indexSetValidator,
                                    Validator validator,
                                    IndexSetTemplateService templateService,
                                    IndexSetDefaultTemplateService indexSetDefaultTemplateService) {
        this.indexSetValidator = indexSetValidator;
        this.validator = validator;
        this.templateService = templateService;
        this.indexSetDefaultTemplateService = indexSetDefaultTemplateService;
    }

    @GET
    @Path("/{template_id}")
    @Timed
    @NoAuditEvent("No change to the DB")
    @ApiOperation(value = "Gets template by id")
    public IndexSetTemplate retrieveById(@ApiParam(name = "template_id") @PathParam("template_id") String templateId) {
        checkPermission(RestPermissions.INDEX_SET_TEMPLATES_READ, templateId);
        return getIndexSetTemplate(templateId);
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
        validateConfig(templateData.indexSetConfig());

        // Templates created via the API are always writable
        return templateService.save(new IndexSetTemplate(templateData, false));
    }

    @PUT
    @Timed
    @AuditEvent(type = INDEX_SET_TEMPLATE_UPDATE)
    @ApiOperation(value = "Updates existing template")
    public void update(@ApiParam(name = "template") IndexSetTemplate template) throws IllegalAccessException {
        checkPermission(RestPermissions.INDEX_SET_TEMPLATES_EDIT, template.id());
        checkReadOnly(getIndexSetTemplate(template.id()));
        validateConfig(template.indexSetConfig());

        final boolean updated = templateService.update(template.id(), template);
        if (!updated) {
            throw new NotFoundException(f("Template %s <%s> does not exist", template.id(), template.title()));
        }
    }

    @DELETE
    @Path("/{template_id}")
    @Timed
    @AuditEvent(type = INDEX_SET_TEMPLATE_DELETE)
    @ApiOperation(value = "Removes a template")
    public void delete(@ApiParam(name = "template_id") @PathParam("template_id") String templateId) throws IllegalAccessException {
        checkPermission(RestPermissions.INDEX_SET_TEMPLATES_DELETE, templateId);
        final IndexSetTemplate template = getIndexSetTemplate(templateId);
        checkReadOnly(template);
        checkIsDefault(template);
        templateService.delete(templateId);
    }

    private void validateConfig(IndexSetTemplateConfig config) {
        // Validate scalar fields.
        validator.validate(config).forEach(v -> {
            throw new BadRequestException(buildFieldError(v.getPropertyPath().toString(), v.getMessage()));
        });

        // Perform common refresh interval and retention period validations.
        IndexSetValidator.Violation violation =
                indexSetValidator.validateRefreshInterval(Duration.standardSeconds(
                        config.fieldTypeRefreshIntervalUnit().toSeconds(config.fieldTypeRefreshInterval())));
        if (violation != null) {
            throw new BadRequestException(buildFieldError(IndexSetTemplateConfig.FIELD_TYPE_REFRESH_INTERVAL, violation.message()));
        }

        violation = indexSetValidator.validateRotation(config.rotationStrategyConfig());
        if (violation != null) {
            throw new BadRequestException(buildFieldError(IndexSetTemplateConfig.ROTATION_STRATEGY_CONFIG, violation.message()));
        }

        violation = indexSetValidator.validateRetentionPeriod(config.rotationStrategyConfig(),
                config.retentionStrategyConfig());
        if (violation != null) {
            throw new BadRequestException(buildFieldError(IndexSetTemplateConfig.RETENTION_STRATEGY_CONFIG, violation.message()));
        }

        violation = indexSetValidator.validateDataTieringConfig(config.dataTiering());
        if (violation != null) {
            throw new BadRequestException(buildFieldError(IndexSetConfig.FIELD_DATA_TIERING, violation.message()));
        }
    }

    private IndexSetTemplate getIndexSetTemplate(String templateId) {
        return templateService.get(templateId)
                .orElseThrow(() -> new NotFoundException(f("No template with id %s", templateId)));
    }

    private static String buildFieldError(String field, String message) {
        return f("Invalid value for field [%s]: %s", field, message);
    }

    private void checkIsDefault(IndexSetTemplate template) throws IllegalAccessException {
        if(indexSetDefaultTemplateService.isDefault(template.id())){
            throw new IllegalAccessException(f("Template %s <%s> is set as default and cannot be deleted", template.id(), template.title()));
        }
    }

    private void checkReadOnly(IndexSetTemplate template) throws IllegalAccessException {
        if (template.isBuiltIn()) {
            throw new IllegalAccessException(f("Template %s <%s> is read-only and cannot be modified or deleted", template, template.title()));
        }
    }
}
