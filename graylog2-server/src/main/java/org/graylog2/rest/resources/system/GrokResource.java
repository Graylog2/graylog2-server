/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.ValidationException;
import org.graylog2.grok.GrokPattern;
import org.graylog2.grok.GrokPatternService;
import org.graylog2.rest.resources.RestResource;

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
import java.util.Set;


@RequiresAuthentication
@Path("/system/grok")
@Produces("application/json")
@Consumes("application/json")
@Api(value = "Grok patterns", description = "Manage grok patterns")
public class GrokResource extends RestResource {

    private final GrokPatternService grokPatternService;

    @Inject
    public GrokResource(GrokPatternService grokPatternService) {
        this.grokPatternService = grokPatternService;
    }

    @GET
    @Timed
    public Set<GrokPattern> listGrokPatterns() {
        return grokPatternService.loadAll();
    }
    
    @GET
    @Timed
    @Path("/{patternId}")
    @ApiOperation("Get the existing grok pattern")
    public GrokPattern listPattern(@ApiParam(name = "patternId", required = true)
                                       @PathParam("patternId") String patternId) throws NotFoundException {
        return grokPatternService.load(patternId);
    }
    
    @POST
    @Timed
    public Response createPattern(@ApiParam(name = "pattern", required = true)
                                      @Valid @NotNull GrokPattern pattern) throws ValidationException {
        final GrokPattern newPattern = grokPatternService.save(pattern);

        final URI patternUri = UriBuilder.fromResource(GrokPattern.class).path("{patternId").build(newPattern._id);
        
        return Response.created(patternUri).entity(newPattern).build();
    }

    @PUT
    @Timed
    public Response bulkUpdatePatterns(@ApiParam(name = "patterns", required = true) @NotNull Set<GrokPattern> patterns) throws ValidationException {
        for (final GrokPattern pattern : patterns) {
            if (!grokPatternService.validate(pattern)) {
                throw new ValidationException("Invalid pattern " + pattern + ". Did not save any patterns.");
            }
        }
        for (GrokPattern pattern : patterns) {
            grokPatternService.save(pattern);
        }
        return Response.accepted().build();
    }
    
    @PUT
    @Timed
    @Path("/{patternId}")
    public GrokPattern updatePattern(@ApiParam(name = "patternId", required = true)
                                     @PathParam("patternId") String patternId,
                                     @ApiParam(name = "pattern", required = true)
                                     GrokPattern pattern) throws NotFoundException, ValidationException {
        final GrokPattern oldPattern = grokPatternService.load(patternId);
        
        oldPattern.name = pattern.name;
        oldPattern.pattern = pattern.pattern;

        final GrokPattern newPattern = grokPatternService.save(oldPattern);
        return newPattern;
    }
    
    @DELETE
    @Timed
    @Path("/{patternId}")
    public void removePattern(@PathParam("patternId") String patternId) {
        if (grokPatternService.delete(patternId) == 0) {
            throw new javax.ws.rs.NotFoundException();
        }
    }
}
