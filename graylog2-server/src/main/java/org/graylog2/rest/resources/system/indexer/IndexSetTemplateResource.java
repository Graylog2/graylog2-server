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
import jakarta.annotation.Nonnull;
import jakarta.inject.Inject;
import jakarta.validation.Validator;
import jakarta.validation.constraints.NotNull;
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
import org.graylog2.indexer.indexset.template.IndexSetDefaultTemplateService;
import org.graylog2.indexer.indexset.template.IndexSetTemplate;
import org.graylog2.indexer.indexset.template.IndexSetTemplateConfig;
import org.graylog2.indexer.indexset.template.IndexSetTemplateRequest;
import org.graylog2.indexer.indexset.template.IndexSetTemplateService;
import org.graylog2.indexer.indexset.template.requirement.IndexSetTemplateRequirement;
import org.graylog2.indexer.indexset.template.requirement.IndexSetTemplateRequirementsChecker;
import org.graylog2.indexer.indexset.template.rest.IndexSetTemplateResponse;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.List;
import java.util.Objects;

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
    private final IndexSetTemplateRequirementsChecker indexSetTemplateRequirementsChecker;

    @Inject
    public IndexSetTemplateResource(IndexSetValidator indexSetValidator,
                                    Validator validator,
                                    IndexSetTemplateService templateService,
                                    IndexSetDefaultTemplateService indexSetDefaultTemplateService,
                                    IndexSetTemplateRequirementsChecker indexSetTemplateRequirementsChecker) {
        this.indexSetValidator = indexSetValidator;
        this.validator = validator;
        this.templateService = templateService;
        this.indexSetDefaultTemplateService = indexSetDefaultTemplateService;
        this.indexSetTemplateRequirementsChecker = indexSetTemplateRequirementsChecker;
    }

    @GET
    @Path("/{template_id}")
    @Timed
    @NoAuditEvent("No change to the DB")
    @ApiOperation(value = "Gets template by id")
    public IndexSetTemplateResponse retrieveById(@ApiParam(name = "template_id") @PathParam("template_id") String templateId) {
        checkPermission(RestPermissions.INDEX_SET_TEMPLATES_READ, templateId);
        return getIndexSetTemplate(templateId);
    }

    @GET
    @Path("/default_config")
    @Timed
    @NoAuditEvent("No change to the DB")
    @ApiOperation(value = "Gets default template")
    public IndexSetTemplateConfig getDefaultConfig() {
        return indexSetDefaultTemplateService.getOrCreateDefaultConfig();
    }

    @GET
    @Path("/paginated")
    @Timed
    @NoAuditEvent("No change to the DB")
    @ApiOperation(value = "Gets template by id")
    public PageListResponse<IndexSetTemplateResponse> getPage(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
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
        return toPaginatedResponse(templateService.getPaginated(query, filters, page, perPage, sort, order));
    }

    @GET
    @Path("/built-in")
    @Timed
    @ApiOperation(value = "Gets built-in templates")
    public List<IndexSetTemplateResponse> builtIns(@ApiParam(name = "warm_tier_enabled")
                                                   @QueryParam("warm_tier_enabled") boolean warmTierEnabled) {
        checkPermission(RestPermissions.INDEX_SET_TEMPLATES_READ);
        return toResponse(templateService.getBuiltIns(warmTierEnabled));
    }

    @POST
    @Timed
    @AuditEvent(type = INDEX_SET_TEMPLATE_CREATE)
    @ApiOperation(value = "Creates a new editable template")
    public IndexSetTemplateResponse create(@ApiParam(name = "request") IndexSetTemplateRequest templateData) {
        checkPermission(RestPermissions.INDEX_SET_TEMPLATES_CREATE);
        validateConfig(templateData.indexSetConfig());

        return toResponse(templateService.save(new IndexSetTemplate(templateData)));
    }

    @PUT
    @Path("{id}")
    @Timed
    @AuditEvent(type = INDEX_SET_TEMPLATE_UPDATE)
    @ApiOperation(value = "Updates existing template")
    public void update(@ApiParam(name = "id", required = true)
                       @PathParam("id") String id,
                       @ApiParam(name = "request")
                       @NotNull IndexSetTemplateRequest template) throws IllegalAccessException {
        checkPermission(RestPermissions.INDEX_SET_TEMPLATES_EDIT, id);
        checkReadOnly(getIndexSetTemplate(id));
        validateConfig(template.indexSetConfig());

        final boolean updated = templateService.update(id, new IndexSetTemplate(template));
        if (!updated) {
            throw new NotFoundException(f("Template %s <%s> does not exist", id, template.title()));
        }
    }

    @DELETE
    @Path("/{template_id}")
    @Timed
    @AuditEvent(type = INDEX_SET_TEMPLATE_DELETE)
    @ApiOperation(value = "Removes a template")
    public void delete(@ApiParam(name = "template_id") @PathParam("template_id") String templateId) throws IllegalAccessException {
        checkPermission(RestPermissions.INDEX_SET_TEMPLATES_DELETE, templateId);
        final IndexSetTemplateResponse template = getIndexSetTemplate(templateId);
        checkReadOnly(template);
        checkIsDefault(template);
        templateService.delete(templateId);
    }

    private void validateConfig(IndexSetTemplateConfig config) {
        IndexSetValidator.Violation violation = indexSetValidator.checkDataTieringNotNull(config.useLegacyRotation(), config.dataTieringConfig());
        if (violation != null) {
            throw new BadRequestException(violation.message());
        }
        // Validate scalar fields.
        validator.validate(config).forEach(v -> {
            throw new BadRequestException(f("Invalid value for field [%s]: %s", v.getPropertyPath().toString(), v.getMessage()));
        });

        // Perform common refresh interval and retention period validations.
        violation = indexSetValidator.validateRefreshInterval(config.fieldTypeRefreshInterval());
        if (violation != null) {
            throw new BadRequestException(violation.message());
        }

        if (config.useLegacyRotation()) {
            violation = indexSetValidator.validateStrategyFields(config);
        } else {
            violation = indexSetValidator.validateDataTieringConfig(config.dataTieringConfig());
        }
        if (violation != null) {
            throw new BadRequestException(violation.message());
        }
    }

    private IndexSetTemplateResponse getIndexSetTemplate(String templateId) {
        return templateService.get(templateId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException(f("No template with id %s", templateId)));
    }

    private @Nonnull IndexSetTemplateResponse toResponse(IndexSetTemplate indexSetTemplate) {
        return toResponse(indexSetTemplate, indexSetDefaultTemplateService.getDefaultIndexSetTemplateId());
    }

    private void checkIsDefault(IndexSetTemplateResponse template) throws IllegalAccessException {
        if (template.isDefault()) {
            throw new IllegalAccessException(f("Template %s <%s> is set as default and cannot be deleted", template.title(), template.id()));
        }
    }

    private void checkReadOnly(IndexSetTemplateResponse template) throws IllegalAccessException {
        if (template.isBuiltIn()) {
            throw new IllegalAccessException(f("Template %s <%s> is read-only and cannot be modified or deleted", template.title(), template.id()));
        }
    }

    private PageListResponse<IndexSetTemplateResponse> toPaginatedResponse(PageListResponse<IndexSetTemplate> pageListResponse) {
        return PageListResponse.create(
                pageListResponse.query(),
                pageListResponse.paginationInfo(),
                pageListResponse.total(),
                pageListResponse.sort(),
                pageListResponse.order(),
                toResponse(pageListResponse.elements()),
                pageListResponse.attributes(),
                pageListResponse.defaults());
    }

    private List<IndexSetTemplateResponse> toResponse(List<IndexSetTemplate> templates) {
        String defaultIndexSetTemplateId = indexSetDefaultTemplateService.getDefaultIndexSetTemplateId();
        return templates.stream().map(indexSetTemplate -> toResponse(indexSetTemplate, defaultIndexSetTemplateId)).toList();
    }

    private IndexSetTemplateResponse toResponse(IndexSetTemplate indexSetTemplate, String defaultTemplateId) {
        IndexSetTemplateRequirement.Result result = indexSetTemplateRequirementsChecker.check(indexSetTemplate);
        return new IndexSetTemplateResponse(
                indexSetTemplate.id(),
                indexSetTemplate.title(),
                indexSetTemplate.description(),
                indexSetTemplate.isBuiltIn(),
                Objects.equals(defaultTemplateId, indexSetTemplate.id()),
                result.fulfilled(),
                result.reason(),
                indexSetTemplate.indexSetConfig()
        );
    }
}
