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
package org.graylog.plugins.views.search.rest;

import io.swagger.annotations.Api;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

@Api(value = "Search/Functions")
@Path("/views/functions")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class PivotSeriesFunctionsResource extends RestResource implements PluginRestResource {
    private final Map<String, SeriesDescription> availableFunctions;

    @Inject
    public PivotSeriesFunctionsResource(Map<String, SeriesDescription> availableFunctions) {
        this.availableFunctions = availableFunctions;
    }

    @GET
    public Map<String, SeriesDescription> functions() {
        return this.availableFunctions;
    }
}
