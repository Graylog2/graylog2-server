package org.graylog2.rest.resources.system.debug;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.indexer.fieldtypes.IndexFieldTypePollerPeriodical;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@RequiresAuthentication
@Api(value = "System/Debug/Field Types", description = "For triggering field type refreshs.")
@Path("/system/debug/field_types")
@Produces(MediaType.APPLICATION_JSON)
public class DebugFieldTypesResource {
    private final IndexFieldTypePollerPeriodical poller;

    public DebugFieldTypesResource(IndexFieldTypePollerPeriodical poller) {
        this.poller = poller;
    }

    @POST
    @Path("/refresh")
    @ApiOperation(value = "Get information about currently active stream router engine.")
    @Produces(MediaType.APPLICATION_JSON)
    public Response triggerFieldTypesRefresh() {
        this.poller.doRun();

        return Response.ok().build();
    }
}
