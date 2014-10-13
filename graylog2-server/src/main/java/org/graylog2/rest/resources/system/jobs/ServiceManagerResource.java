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
package org.graylog2.rest.resources.system.jobs;

import com.codahale.metrics.annotation.Timed;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.graylog2.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@RequiresAuthentication
@Api(value = "System/ServiceManager", description = "ServiceManager Status")
@Path("/system/serviceManager")
public class ServiceManagerResource extends RestResource {
    private final ServiceManager serviceManager;

    @Inject
    public ServiceManagerResource(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    @GET
    @Timed
    @ApiOperation(value = "List current status of ServiceManager")
    @Produces(MediaType.APPLICATION_JSON)
    public String list() {
        Map<Service, Long> result = serviceManager.startupTimes();
        return json(result);
    }
}
