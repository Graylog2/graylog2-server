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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.ImmutableMap;
import io.krakens.grok.api.exception.GrokException;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PaginatedList;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.grok.PaginatedGrokPatternService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.models.PaginatedResponse;
import org.graylog2.rest.models.system.grokpattern.requests.GrokPatternTestRequest;
import org.graylog2.rest.models.system.responses.GrokPatternList;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RequiresAuthentication
@Path("/system/grok")
@Produces("application/json")
@Consumes("application/json")
@Api(value = "System/Grok", description = "Manage grok patterns")
public class GrokResource extends RestResource {
    private static final Pattern GROK_LINE_PATTERN = Pattern.compile("^(\\w+)[ \t]+(.*)$");

    private static final ImmutableMap<String, SearchQueryField> SEARCH_FIELD_MAPPING = ImmutableMap.<String, SearchQueryField>builder()
            .put(GrokPattern.FIELD_NAME, SearchQueryField.create(GrokPattern.FIELD_NAME))
            .put(GrokPattern.FIELD_PATTERN, SearchQueryField.create(GrokPattern.FIELD_PATTERN))
            .build();

    private final GrokPatternService grokPatternService;
    private final SearchQueryParser searchQueryParser;
    private final PaginatedGrokPatternService paginatedGrokPatternService;

    @Inject
    public GrokResource(GrokPatternService grokPatternService,
                        PaginatedGrokPatternService paginatedGrokPatternService) {

        this.paginatedGrokPatternService = paginatedGrokPatternService;
        this.grokPatternService = grokPatternService;
        this.searchQueryParser = new SearchQueryParser(GrokPattern.FIELD_NAME, SEARCH_FIELD_MAPPING);
    }

    @GET
    @Timed
    @Deprecated
    @ApiOperation("Get all existing grok patterns")
    public GrokPatternList listGrokPatterns() {
        checkPermission(RestPermissions.INPUTS_READ);

        return GrokPatternList.create(grokPatternService.loadAll());
    }

    @GET
    @Timed
    @Path("/paginated")
    @ApiOperation("Get existing grok patterns paged")
    @Produces(MediaType.APPLICATION_JSON)
    public PaginatedResponse<GrokPattern> getPage(@ApiParam(name = "page") @QueryParam("page") @DefaultValue("1") int page,
                                     @ApiParam(name = "per_page") @QueryParam("per_page") @DefaultValue("50") int perPage,
                                     @ApiParam(name = "query") @QueryParam("query") @DefaultValue("") String query,
                                     @ApiParam(name = "sort",
                                               value = "The field to sort the result on",
                                               required = true,
                                               allowableValues = "title,description,id")
                                       @DefaultValue(GrokPattern.FIELD_NAME) @QueryParam("sort") String sort,
                                     @ApiParam(name = "order", value = "The sort direction", allowableValues = "asc, desc")
                                       @DefaultValue("asc") @QueryParam("order") String order) {
        checkPermission(RestPermissions.INPUTS_READ);

        SearchQuery searchQuery;
        try {
            searchQuery = searchQueryParser.parse(query);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Invalid argument in search query: " + e.getMessage());
        }
        final PaginatedList<GrokPattern> result = paginatedGrokPatternService
                .findPaginated(searchQuery, page, perPage, sort, order);

        return PaginatedResponse.create("patterns", result);
    }

    @GET
    @Timed
    @Path("/{patternId}")
    @ApiOperation("Get the existing grok pattern")
    public GrokPattern listPattern(@ApiParam(name = "patternId", required = true)
                                       @PathParam("patternId") String patternId) throws NotFoundException {
        checkPermission(RestPermissions.INPUTS_READ);

        return grokPatternService.load(patternId);
    }

    @POST
    @Timed
    @Path("/test")
    @NoAuditEvent("Only used to test pattern.")
    @ApiOperation(value = "Test pattern with sample data")
    public Response testPattern(@ApiParam(name = "pattern", required = true) GrokPatternTestRequest request) {
        Map<String, Object> result;
        try {
            result = grokPatternService.match(request.grokPattern(), request.sampleData());
        } catch (GrokException | IllegalArgumentException e) {
            Map<String, String> error = ImmutableMap.of("message", e.getMessage());

            throw new BadRequestException(Response.status(Response.Status.BAD_REQUEST).entity(error).build());
        }
        return Response.ok(result).build();
    }

    @POST
    @Timed
    @ApiOperation(value = "Add a new named pattern", response = GrokPattern.class)
    @AuditEvent(type = AuditEventTypes.GROK_PATTERN_CREATE)
    public Response createPattern(@ApiParam(name = "pattern", required = true)
                                      @Valid @NotNull GrokPattern pattern) throws ValidationException {
        checkPermission(RestPermissions.INPUTS_CREATE);

        // remove the ID from the pattern, this is only used to create new patterns
        final GrokPattern newPattern = grokPatternService.save(pattern.toBuilder().id(null).build());

        final URI patternUri = getUriBuilderToSelf().path(GrokResource.class, "listPattern").build(newPattern.id());

        return Response.created(patternUri).entity(newPattern).build();
    }

    @PUT
    @Timed
    @ApiOperation("Add a list of new patterns")
    @AuditEvent(type = AuditEventTypes.GROK_PATTERN_IMPORT_CREATE)
    public Response bulkUpdatePatterns(@ApiParam(name = "patterns", required = true) @NotNull GrokPatternList patternList,
                                       @ApiParam(name = "replace", value = "Replace all patterns with the new ones.")
                                       @QueryParam("replace") @DefaultValue("false") boolean replace) throws ValidationException {
        checkPermission(RestPermissions.INPUTS_CREATE);

        try {
            if (!grokPatternService.validateAll(patternList.patterns())) {
                throw new ValidationException("Invalid pattern contained. Did not save any patterns.");
            }
        } catch (GrokException | IllegalArgumentException e) {
            throw new ValidationException("Invalid pattern. Did not save any patterns\n" + e.getMessage());
        }

        grokPatternService.saveAll(patternList.patterns(), replace);
        return Response.accepted().build();
    }

    @POST
    @Consumes(MediaType.TEXT_PLAIN)
    @Timed
    @ApiOperation("Add a list of new patterns")
    @AuditEvent(type = AuditEventTypes.GROK_PATTERN_IMPORT_CREATE)
    public Response bulkUpdatePatternsFromTextFile(@ApiParam(name = "patterns", required = true) @NotNull InputStream patternsFile,
                                                   @ApiParam(name = "replace", value = "Replace all patterns with the new ones.")
                                                   @QueryParam("replace") @DefaultValue("false") boolean replace) throws ValidationException, IOException {
        checkPermission(RestPermissions.INPUTS_CREATE);

        final List<GrokPattern> grokPatterns = readGrokPatterns(patternsFile);

        if (!grokPatterns.isEmpty()) {
            try {
                if (!grokPatternService.validateAll(grokPatterns)) {
                    throw new ValidationException("Invalid pattern contained. Did not save any patterns.");
                }
            } catch (GrokException | IllegalArgumentException e) {
                throw new ValidationException("Invalid pattern. Did not save any patterns\n" + e.getMessage());
            }

            grokPatternService.saveAll(grokPatterns, replace);
        }

        return Response.accepted().build();
    }

    private List<GrokPattern> readGrokPatterns(InputStream patternList) throws IOException {
        try (final InputStreamReader inputStreamReader = new InputStreamReader(patternList, StandardCharsets.UTF_8);
             final BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            return bufferedReader.lines()
                    .map(String::trim)
                    .filter(s -> !s.startsWith("#") && !s.isEmpty())
                    .map(GROK_LINE_PATTERN::matcher)
                    .filter(Matcher::matches)
                    .map(matcher -> GrokPattern.create(matcher.group(1), matcher.group(2)))
                    .collect(Collectors.toList());

        }
    }

    @PUT
    @Timed
    @Path("/{patternId}")
    @ApiOperation("Update an existing pattern")
    @AuditEvent(type = AuditEventTypes.GROK_PATTERN_UPDATE)
    public GrokPattern updatePattern(@ApiParam(name = "patternId", required = true)
                                     @PathParam("patternId") String patternId,
                                     @ApiParam(name = "pattern", required = true)
                                     GrokPattern pattern) throws ValidationException {
        checkPermission(RestPermissions.INPUTS_EDIT);

        final GrokPattern grokPattern = GrokPattern.builder()
                .id(patternId)
                .name(pattern.name())
                .pattern(pattern.pattern())
                .build();

        return grokPatternService.update(grokPattern);
    }

    @DELETE
    @Timed
    @Path("/{patternId}")
    @ApiOperation("Remove an existing pattern by id")
    @AuditEvent(type = AuditEventTypes.GROK_PATTERN_DELETE)
    public void removePattern(@ApiParam(name = "patternId", required = true) @PathParam("patternId") String patternId) throws NotFoundException {
        checkPermission(RestPermissions.INPUTS_EDIT);

        grokPatternService.load(patternId);
        if (grokPatternService.delete(patternId) == 0) {
            throw new javax.ws.rs.NotFoundException("Couldn't remove Grok pattern with ID " + patternId);
        }
    }
}
