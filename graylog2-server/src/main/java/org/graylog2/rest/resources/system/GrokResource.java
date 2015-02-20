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
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.NotFoundException;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.rest.resources.system.responses.GrokPatternList;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.net.URI;


@RequiresAuthentication
@Path("/system/grok")
@Produces("application/json")
@Consumes("application/json")
@Api(value = "System/Grok", description = "Manage grok patterns")
public class GrokResource extends RestResource {

    private final GrokPatternService grokPatternService;

    @Inject
    public GrokResource(GrokPatternService grokPatternService) {
        this.grokPatternService = grokPatternService;
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
    @ApiOperation("Add a new named pattern")
    public Response createPattern(@ApiParam(name = "pattern", required = true)
                                      @Valid @NotNull GrokPattern pattern) throws ValidationException {
        checkPermission(RestPermissions.INPUTS_CREATE);

        final GrokPattern newPattern = grokPatternService.save(pattern);

        final URI patternUri = UriBuilder.fromMethod(GrokResource.class, "listPattern").build(newPattern.id);
        
        return Response.created(patternUri).entity(newPattern).build();
    }

    @PUT
    @Timed
    @ApiOperation("Add a list of new patterns")
    public Response bulkUpdatePatterns(@ApiParam(name = "patterns", required = true) @NotNull GrokPatternList patternList) throws ValidationException {
        checkPermission(RestPermissions.INPUTS_CREATE);

        for (final GrokPattern pattern : patternList.patterns()) {
            if (!grokPatternService.validate(pattern)) {
                throw new ValidationException("Invalid pattern " + pattern + ". Did not save any patterns.");
            }
        }
        for (final GrokPattern pattern : patternList.patterns()) {
            grokPatternService.save(pattern);
        }
        return Response.accepted().build();
    }
    
    @PUT
    @Timed
    @Path("/{patternId}")
    @ApiOperation("Update an existing pattern")
    public GrokPattern updatePattern(@ApiParam(name = "patternId", required = true)
                                     @PathParam("patternId") String patternId,
                                     @ApiParam(name = "pattern", required = true)
                                     GrokPattern pattern) throws NotFoundException, ValidationException {
        checkPermission(RestPermissions.INPUTS_EDIT);
        
        final GrokPattern oldPattern = grokPatternService.load(patternId);
        
        oldPattern.name = pattern.name;
        oldPattern.pattern = pattern.pattern;

        return grokPatternService.save(oldPattern);
    }
    
    @DELETE
    @Timed
    @Path("/{patternId}")
    @ApiOperation("Remove an existing pattern by id")
    public void removePattern(@PathParam("patternId") String patternId) {
        checkPermission(RestPermissions.INPUTS_EDIT);

        if (grokPatternService.delete(patternId) == 0) {
            throw new javax.ws.rs.NotFoundException();
        }
    }
}
