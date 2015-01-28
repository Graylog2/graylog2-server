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
package org.graylog2.shared.rest.resources.documentation;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.graylog2.plugin.BaseConfiguration;
import org.graylog2.shared.rest.documentation.generator.Generator;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

@Api(value = "Documentation", description = "Documentation of this API in JSON format.")
@Path("/api-docs")
public class DocumentationResource extends RestResource {

    private BaseConfiguration configuration;
    private final String[] restControllerPackages;

    @Inject
    public DocumentationResource(BaseConfiguration configuration, @Named("RestControllerPackages") String[] restControllerPackages) {
        this.configuration = configuration;
        this.restControllerPackages = restControllerPackages;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get API documentation")
    @Produces(MediaType.APPLICATION_JSON)
    public Response overview() {
        return buildSuccessfulCORSResponse(new Generator(restControllerPackages, objectMapper).generateOverview());
    }

    @GET
    @Timed
    @ApiOperation(value = "Get detailed API documentation of a single resource")
    @Produces(MediaType.APPLICATION_JSON)
    @Path("/{route: .+}")
    public Response route(@ApiParam(name = "route", value = "Route to fetch. For example /system", required = true)
                          @PathParam("route") String route) {
        return buildSuccessfulCORSResponse(
                new Generator(restControllerPackages, objectMapper).generateForRoute(route, configuration.getRestTransportUri().toString())
        );
    }

    private Response buildSuccessfulCORSResponse(Map<String, Object> result) {
        return Response.ok(result)
                .header("Access-Control-Allow-Origin", "*") // Headers for Swagger UI.
                .header("Access-Control-Allow-Methods", "GET")
                .header("Access-Control-Allow-Headers", "Content-Type, api_key, Authorization")
                .build();
    }
}
