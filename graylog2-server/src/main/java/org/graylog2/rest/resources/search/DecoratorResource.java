package org.graylog2.rest.resources.search;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.decorators.Decorator;
import org.graylog2.decorators.DecoratorImpl;
import org.graylog2.decorators.DecoratorService;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;

@RequiresAuthentication
@Api(value = "Search/Decorators", description = "Message search decorators")
@Path("/search/decorators")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DecoratorResource extends RestResource {
    private final DecoratorService decoratorService;

    @Inject
    public DecoratorResource(DecoratorService decoratorService) {
        this.decoratorService = decoratorService;
    }

    @GET
    @Timed
    @ApiOperation(value = "Returns all configured message decorations",
        notes = "This ia mainly used for bulk loading of message decorator configurations")
    public List<Decorator> get() {
        return this.decoratorService.findAll();
    }

    @POST
    @Timed
    @ApiOperation(value = "Creates a message decoration configuration")
    public Decorator create(@ApiParam(name = "JSON body", required = true) DecoratorImpl decorator) {
        return this.decoratorService.save(decorator);
    }
}
