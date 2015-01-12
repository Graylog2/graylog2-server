package org.graylog2.rest.resources.system;

import com.codahale.metrics.annotation.Timed;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
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

    @Inject
    public SystemShutdownResource(GracefulShutdown gracefulShutdown) {
        this.gracefulShutdown = gracefulShutdown;
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
