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
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.Configuration;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.configuration.ExposedConfiguration;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@RequiresAuthentication
@Api(value = "System/Configuration", description = "Read-only access to configuration settings")
@Path("/system/configuration")
@Produces(MediaType.APPLICATION_JSON)
public class ConfigurationResource extends RestResource {

    private final Configuration configuration;
    private final ElasticsearchConfiguration esConfiguration;

    @Inject
    public ConfigurationResource(Configuration configuration, ElasticsearchConfiguration esConfiguration) {
        this.configuration = configuration;
        this.esConfiguration = esConfiguration;
    }

    @GET
    @Timed
    @ApiOperation(value = "Get relevant configuration settings and their values")
    public ExposedConfiguration getRelevant() {
        return ExposedConfiguration.create(configuration, esConfiguration);
    }
}
