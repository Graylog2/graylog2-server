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
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.Configuration;
import org.graylog2.configuration.ElasticsearchConfiguration;
import org.graylog2.configuration.ExposedConfiguration;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

@RequiresAuthentication
@Api(value = "System/Configuration", description = "Read-only access to configuration variables")
@Path("/system/configuration")
public class ConfigurationResource extends RestResource {

    private final Configuration configuration;
    private final ElasticsearchConfiguration esConfiguration;

    @Inject
    public ConfigurationResource(Configuration configuration, ElasticsearchConfiguration esConfiguration) {
        this.configuration = configuration;
        this.esConfiguration = esConfiguration;
    }

    /*
     * This call is returning a list of configuration values that are safe to return, i.e. do not include any sensitive
     * information.
     */
    @GET
    @Timed
    @ApiOperation(value = "Get relevant configuration variables and their values")
    public ExposedConfiguration getRelevant() {
        return ExposedConfiguration.create(configuration, esConfiguration);
    }
}
