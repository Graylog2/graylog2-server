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
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.events.ClusterEventBus;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.grok.GrokPatternsChangedEvent;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.models.system.responses.GrokPatternList;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
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
import java.util.Collections;
import java.util.List;
import java.util.Set;
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

    private final GrokPatternService grokPatternService;
    private final ClusterEventBus clusterBus;

    @Inject
    public GrokResource(GrokPatternService grokPatternService, ClusterEventBus clusterBus) {
        this.grokPatternService = grokPatternService;
        this.clusterBus = clusterBus;
    }

    @GET
    @Timed
    @ApiOperation("Get all existing grok patterns")
    public GrokPatternList listGrokPatterns() {
        checkPermission(RestPermissions.INPUTS_READ);

        return GrokPatternList.create(grokPatternService.loadAll());
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
    @ApiOperation(value = "Add a new named pattern", response = GrokPattern.class)
    @AuditEvent(type = AuditEventTypes.GROK_PATTERN_CREATE)
    public Response createPattern(@ApiParam(name = "pattern", required = true)
                                      @Valid @NotNull GrokPattern pattern) throws ValidationException {
        checkPermission(RestPermissions.INPUTS_CREATE);

        // remove the ID from the pattern, this is only used to create new patterns
        final GrokPattern newPattern = grokPatternService.save(pattern.toBuilder().id(null).build());

        clusterBus.post(GrokPatternsChangedEvent.create(Collections.emptySet(), Sets.newHashSet(newPattern.name())));

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

        final Set<String> updatedPatternNames = Sets.newHashSet();
        for (final GrokPattern pattern : patternList.patterns()) {
            updatedPatternNames.add(pattern.name());
            if (!grokPatternService.validate(pattern)) {
                throw new ValidationException("Invalid pattern " + pattern + ". Did not save any patterns.");
            }
        }

        grokPatternService.saveAll(patternList.patterns(), replace);
        clusterBus.post(GrokPatternsChangedEvent.create(Collections.emptySet(), updatedPatternNames));
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
            final Set<String> updatedPatternNames = Sets.newHashSetWithExpectedSize(grokPatterns.size());
            for (final GrokPattern pattern : grokPatterns) {
                updatedPatternNames.add(pattern.name());
                if (!grokPatternService.validate(pattern)) {
                    throw new ValidationException("Invalid pattern " + pattern + ". Did not save any patterns.");
                }
            }

            grokPatternService.saveAll(grokPatterns, replace);
            clusterBus.post(GrokPatternsChangedEvent.create(Collections.emptySet(), updatedPatternNames));
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
                                     GrokPattern pattern) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.INPUTS_EDIT);

        final GrokPattern oldPattern = grokPatternService.load(patternId);

        final Set<String> deletedNames = Sets.newHashSet(oldPattern.name());
        final Set<String> updatedNames = Sets.newHashSet(pattern.name());

        final GrokPattern toSave = oldPattern.toBuilder()
                .name(pattern.name())
                .pattern(pattern.pattern())
                .build();

        clusterBus.post(GrokPatternsChangedEvent.create(deletedNames, updatedNames));
        return grokPatternService.save(toSave);
    }

    @DELETE
    @Timed
    @Path("/{patternId}")
    @ApiOperation("Remove an existing pattern by id")
    @AuditEvent(type = AuditEventTypes.GROK_PATTERN_DELETE)
    public void removePattern(@ApiParam(name = "patternId", required = true) @PathParam("patternId") String patternId) throws NotFoundException {
        checkPermission(RestPermissions.INPUTS_EDIT);
        final GrokPattern pattern = grokPatternService.load(patternId);

        clusterBus.post(GrokPatternsChangedEvent.create(Sets.newHashSet(pattern.name()), Collections.emptySet()));

        if (grokPatternService.delete(patternId) == 0) {
            throw new javax.ws.rs.NotFoundException("Couldn't remove Grok pattern with ID " + patternId);
        }
    }
}
