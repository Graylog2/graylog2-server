package org.graylog2.rest.resources.search;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog2.decorators.Decorator;
import org.graylog2.decorators.DecoratorImpl;
import org.graylog2.decorators.DecoratorService;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.decorators.MessageDecorator;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiresAuthentication
@Api(value = "Search/Decorators", description = "Message search decorators")
@Path("/search/decorators")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DecoratorResource extends RestResource {
    private final DecoratorService decoratorService;
    private final Map<String, MessageDecorator.Factory> messageDecorators;

    @Inject
    public DecoratorResource(DecoratorService decoratorService,
                             Map<String, MessageDecorator.Factory> messageDecorators) {
        this.decoratorService = decoratorService;
        this.messageDecorators = messageDecorators;
    }

    @GET
    @Timed
    @ApiOperation(value = "Returns all configured message decorations",
        notes = "")
    public List<Decorator> get() {
        return this.decoratorService.findAll();
    }

    @GET
    @Timed
    @Path("/available")
    @ApiOperation(value = "Returns all available message decorations",
        notes = "")
    public Map<String, ConfigurationRequest> getAvailable() {
        return this.messageDecorators.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().getConfig().getRequestedConfiguration()));
    }

    @POST
    @Timed
    @ApiOperation(value = "Creates a message decoration configuration")
    public Decorator create(@ApiParam(name = "JSON body", required = true) DecoratorImpl decorator) {
        return this.decoratorService.save(decorator);
    }

    @DELETE
    @Path("/{decoratorId}")
    @Timed
    @ApiOperation(value = "Create a decorator")
    @RequiresPermissions(RestPermissions.STREAMS_CREATE)
    public void create(@ApiParam(name = "decorator id", required = true) @PathParam("decoratorId") final String decoratorId) {
        this.decoratorService.delete(decoratorId);
    }
}
