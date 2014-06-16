package org.graylog2.rest.resources.system.jobs;

import com.codahale.metrics.annotation.Timed;
import com.google.common.util.concurrent.Service;
import com.google.common.util.concurrent.ServiceManager;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.rest.documentation.annotations.Api;
import org.graylog2.rest.documentation.annotations.ApiOperation;
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
