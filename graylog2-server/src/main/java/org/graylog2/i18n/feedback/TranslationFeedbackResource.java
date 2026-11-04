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
package org.graylog2.i18n.feedback;

import com.codahale.metrics.annotation.Timed;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
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
import jakarta.ws.rs.core.Response;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.bson.conversions.Bson;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.DbQueryCreator;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.rest.models.tools.responses.PageListResponse;
import org.graylog2.rest.resources.entities.EntityAttribute;
import org.graylog2.rest.resources.entities.EntityDefaults;
import org.graylog2.rest.resources.entities.FilterOption;
import org.graylog2.rest.resources.entities.Sorting;
import org.graylog2.search.SearchQueryField;
import org.graylog2.shared.rest.resources.RestResource;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_CATEGORY;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_COMMENT;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_CREATED_AT;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_ENGLISH_TEXT;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_KEY;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_LANGUAGE;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_NAMESPACE;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_PATH;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_REPORTER;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_STATUS;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_SUGGESTION;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_TEXT;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.FIELD_UPDATED_AT;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.STATUS_FIXED;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.STATUS_OPEN;
import static org.graylog2.i18n.feedback.TranslationFeedbackDTO.STATUS_WONT_FIX;

/**
 * Collects reports about translations of the web interface from proofreaders.
 * Everybody who is logged in can report and see the reports, only admins can change or delete them.
 */
@RequiresAuthentication
@Path("/system/translation_feedback")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "System/TranslationFeedback", description = "Reports about translations of the web interface")
public class TranslationFeedbackResource extends RestResource {
    private static final String ADMIN_PERMISSION = "*";
    private static final Set<String> STATUSES = Set.of(STATUS_OPEN, STATUS_FIXED, STATUS_WONT_FIX);
    private static final Set<String> CATEGORIES = Set.of("wrong", "untranslated", "awkward", "layout", "other");
    private static final Set<String> LANGUAGES = Set.of("en", "de", "es", "fr", "it", "pl", "pt-BR", "zh-CN", "zh-TW", "ja");
    private static final Set<String> SORT_FIELDS = Set.of(FIELD_LANGUAGE, FIELD_NAMESPACE, FIELD_KEY, FIELD_TEXT, FIELD_CATEGORY,
            FIELD_REPORTER, FIELD_STATUS, FIELD_CREATED_AT, FIELD_UPDATED_AT);
    private static final String DEFAULT_SORT_FIELD = FIELD_CREATED_AT;

    static final List<EntityAttribute> ATTRIBUTES = List.of(
            EntityAttribute.builder().id("id").title("ID").type(SearchQueryField.Type.OBJECT_ID).hidden(true).build(),
            EntityAttribute.builder().id(FIELD_LANGUAGE).title("Language").sortable(true).filterable(true)
                    .filterOptions(options(LANGUAGES)).build(),
            EntityAttribute.builder().id(FIELD_NAMESPACE).title("Namespace").sortable(true).searchable(true).build(),
            EntityAttribute.builder().id(FIELD_KEY).title("Key").sortable(true).searchable(true).build(),
            EntityAttribute.builder().id(FIELD_TEXT).title("Text").sortable(true).searchable(true).build(),
            EntityAttribute.builder().id(FIELD_ENGLISH_TEXT).title("English").searchable(true).build(),
            EntityAttribute.builder().id(FIELD_SUGGESTION).title("Suggestion").searchable(true).build(),
            EntityAttribute.builder().id(FIELD_COMMENT).title("Comment").searchable(true).build(),
            EntityAttribute.builder().id(FIELD_CATEGORY).title("Category").sortable(true).filterable(true)
                    .filterOptions(options(CATEGORIES)).build(),
            EntityAttribute.builder().id(FIELD_PATH).title("Page").build(),
            EntityAttribute.builder().id(FIELD_REPORTER).title("Reporter").sortable(true).searchable(true).filterable(true).build(),
            EntityAttribute.builder().id(FIELD_STATUS).title("Status").sortable(true).filterable(true)
                    .filterOptions(options(STATUSES)).build(),
            EntityAttribute.builder().id(FIELD_CREATED_AT).title("Created").type(SearchQueryField.Type.DATE).sortable(true).filterable(true).build(),
            EntityAttribute.builder().id(FIELD_UPDATED_AT).title("Updated").type(SearchQueryField.Type.DATE).sortable(true).hidden(true).build()
    );

    static final EntityDefaults DEFAULTS = EntityDefaults.builder()
            .sort(Sorting.create(DEFAULT_SORT_FIELD, Sorting.Direction.DESC))
            .build();

    private final TranslationFeedbackService service;
    private final DbQueryCreator dbQueryCreator;

    @Inject
    public TranslationFeedbackResource(TranslationFeedbackService service) {
        this.service = service;
        this.dbQueryCreator = new DbQueryCreator(FIELD_TEXT, ATTRIBUTES);
    }

    private static Set<FilterOption> options(Set<String> values) {
        return values.stream().map(value -> FilterOption.create(value, value)).collect(Collectors.toSet());
    }

    @GET
    @Timed
    @Path("/paginated")
    @Operation(summary = "Get a paginated list of translation reports")
    public PageListResponse<TranslationFeedbackDTO> getPage(@Parameter(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                                            @Parameter(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                                            @Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                                            @Parameter(name = "filters") @QueryParam("filters") List<String> filters,
                                                            @Parameter(name = "sort", description = "The field to sort the result on",
                                                                       schema = @Schema(allowableValues = {"language", "namespace", "key", "text", "category", "reporter", "status", "created_at", "updated_at"}))
                                                            @DefaultValue(DEFAULT_SORT_FIELD) @QueryParam("sort") String sort,
                                                            @Parameter(name = "order", description = "The sort direction",
                                                                       schema = @Schema(allowableValues = {"asc", "desc"}))
                                                            @DefaultValue("desc") @QueryParam("order") SortOrder order) {
        final String sortField = SORT_FIELDS.contains(sort) ? sort : DEFAULT_SORT_FIELD;
        final PaginatedList<TranslationFeedbackDTO> result = service.findPaginated(dbQuery(filters, query), sortField, order, page, perPage);

        return PageListResponse.create(query, result.pagination(), result.pagination().total(), sortField, order, result, ATTRIBUTES, DEFAULTS);
    }

    @POST
    @Timed
    @Operation(summary = "Report a translation")
    @NoAuditEvent("Feedback for proofreading, no configuration change")
    public TranslationFeedbackDTO create(@Valid @NotNull CreateTranslationFeedbackRequest request) {
        if (!CATEGORIES.contains(request.category())) {
            throw new BadRequestException("Unknown category: " + request.category());
        }

        return service.create(request, getCurrentUser().getName());
    }

    @PUT
    @Timed
    @Path("/{id}/status")
    @Operation(summary = "Change the status of a translation report")
    @RequiresPermissions(ADMIN_PERMISSION)
    @NoAuditEvent("Feedback for proofreading, no configuration change")
    public TranslationFeedbackDTO updateStatus(@PathParam("id") String id,
                                               @Valid @NotNull UpdateTranslationFeedbackStatusRequest request) {
        if (!STATUSES.contains(request.status())) {
            throw new BadRequestException("Unknown status: " + request.status());
        }

        return service.updateStatus(id, request.status())
                .orElseThrow(() -> new NotFoundException("Translation report " + id + " not found"));
    }

    @DELETE
    @Timed
    @Path("/{id}")
    @Operation(summary = "Delete a translation report")
    @RequiresPermissions(ADMIN_PERMISSION)
    @NoAuditEvent("Feedback for proofreading, no configuration change")
    public void delete(@PathParam("id") String id) {
        if (!service.delete(id)) {
            throw new NotFoundException("Translation report " + id + " not found");
        }
    }

    @GET
    @Timed
    @Path("/export/json")
    @Operation(summary = "Export the translation reports grouped by language, namespace and key")
    public Response exportJson(@Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
                               @Parameter(name = "filters") @QueryParam("filters") List<String> filters) {
        final TranslationFeedbackExport export = TranslationFeedbackExport.of(service.findAll(dbQuery(filters, query)),
                DateTime.now(DateTimeZone.UTC));

        return attachment(Response.ok(export, MediaType.APPLICATION_JSON_TYPE), "json");
    }

    @GET
    @Timed
    @Path("/export/csv")
    @Produces("text/csv")
    @Operation(summary = "Export the translation reports as CSV, one row per report")
    public Response exportCsv(@Parameter(name = "query") @QueryParam("query") @DefaultValue("") String query,
                              @Parameter(name = "filters") @QueryParam("filters") List<String> filters) {
        final String csv = TranslationFeedbackExport.toCsv(service.findAll(dbQuery(filters, query)));

        return attachment(Response.ok(csv, "text/csv; charset=utf-8"), "csv");
    }

    private Bson dbQuery(List<String> filters, String query) {
        try {
            return dbQueryCreator.createDbQuery(filters == null ? List.of() : filters, query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage(), e);
        }
    }

    private static Response attachment(Response.ResponseBuilder builder, String extension) {
        return builder.header("Content-Disposition", "attachment; filename=\"translation-feedback." + extension + "\"").build();
    }
}
