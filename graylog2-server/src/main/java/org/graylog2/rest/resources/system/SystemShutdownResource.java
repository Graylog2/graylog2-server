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
import org.graylog2.plugin.ServerStatus;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;
import org.graylog2.system.shutdown.GracefulShutdown;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import static javax.ws.rs.core.Response.accepted;

@RequiresAuthentication
@Api(value = "System/Shutdown", description = "Shutdown this node gracefully.")
@Path("/system/shutdown")
public class SystemShutdownResource extends RestResource {
    private final GracefulShutdown gracefulShutdown;
    private final ServerStatus serverStatus;

    @Inject
    public SystemShutdownResource(GracefulShutdown gracefulShutdown,
                                  ServerStatus serverStatus) {
        this.gracefulShutdown = gracefulShutdown;
        this.serverStatus = serverStatus;
    }

    @POST
    @Timed
    @ApiOperation(value = "Shutdown this node gracefully.",
            notes = "Attempts to process all buffered and cached messages before exiting, " +
                    "shuts down inputs first to make sure that no new messages are accepted.")
    @Path("/shutdown")
    public Response shutdown() {
        checkPermission(RestPermissions.NODE_SHUTDOWN, serverStatus.getNodeId().toString());

        new Thread(gracefulShutdown).start();
        return accepted().build();
    }
}
