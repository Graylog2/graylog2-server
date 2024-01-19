package org.graylog2.datatiering;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.shared.rest.resources.RestResource;

@Api(value = "DataTiering", description = "Data tiering management")
@Path("/datatiering")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class DataTieringResource extends RestResource {

    private final DataTieringOrchestrator dataTieringOrchestrator;

    @Inject
    public DataTieringResource(DataTieringOrchestrator dataTieringOrchestrator) {
        this.dataTieringOrchestrator = dataTieringOrchestrator;
    }

    @GET
    @Path("default_config")
    @ApiOperation(value = "Get default configuration.")
    @Produces(MediaType.APPLICATION_JSON)
    public DataTieringConfig defaultConfig() {
        return dataTieringOrchestrator.defaultConfig();
    }
}
