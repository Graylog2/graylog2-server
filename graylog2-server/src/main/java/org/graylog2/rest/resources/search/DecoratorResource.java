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
package org.graylog2.rest.resources.search;

import com.codahale.metrics.annotation.Timed;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.database.NotFoundException;
import org.graylog2.decorators.Decorator;
import org.graylog2.decorators.DecoratorImpl;
import org.graylog2.decorators.DecoratorService;
import org.graylog2.plugin.configuration.ConfigurableTypeInfo;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
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
    private final Map<String, SearchResponseDecorator.Factory> searchResponseDecorators;

    @Inject
    public DecoratorResource(DecoratorService decoratorService,
                             Map<String, SearchResponseDecorator.Factory> searchResponseDecorators) {
        this.decoratorService = decoratorService;
        this.searchResponseDecorators = searchResponseDecorators;
    }

    @GET
    @Timed
    @ApiOperation(value = "Returns all configured message decorations",
        notes = "")
    public List<Decorator> get() {
        checkPermission(RestPermissions.DECORATORS_READ);
        return this.decoratorService.findAll();
    }

    @GET
    @Timed
    @Path("/available")
    @ApiOperation(value = "Returns all available message decorations",
        notes = "")
    public Map<String, ConfigurableTypeInfo> getAvailable() {
        return this.searchResponseDecorators.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey, entry -> ConfigurableTypeInfo.create(
                    entry.getKey(),
                    entry.getValue().getDescriptor(),
                    entry.getValue().getConfig().getRequestedConfiguration()
                )
            ));
    }

    @POST
    @Timed
    @ApiOperation(value = "Creates a message decoration configuration")
    @AuditEvent(type = AuditEventTypes.MESSAGE_DECORATOR_CREATE)
    public Decorator create(@ApiParam(name = "JSON body", required = true) DecoratorImpl decorator) {
        checkPermission(RestPermissions.DECORATORS_CREATE);
        if (decorator.stream().isPresent()) {
            checkPermission(RestPermissions.STREAMS_EDIT, decorator.stream().get());
        }
        return this.decoratorService.save(decorator);
    }

    @DELETE
    @Path("/{decoratorId}")
    @Timed
    @ApiOperation(value = "Create a decorator")
    @AuditEvent(type = AuditEventTypes.MESSAGE_DECORATOR_DELETE)
    public void delete(@ApiParam(name = "decorator id", required = true) @PathParam("decoratorId") final String decoratorId) throws NotFoundException {
        checkPermission(RestPermissions.DECORATORS_EDIT);
        final Decorator decorator = this.decoratorService.findById(decoratorId);

        if (decorator.stream().isPresent()) {
            checkPermission(RestPermissions.STREAMS_EDIT, decorator.stream().get());
        }
        this.decoratorService.delete(decoratorId);
    }

    @PUT
    @Path("/{decoratorId}")
    @Timed
    @ApiOperation(value = "Update a decorator")
    @AuditEvent(type = AuditEventTypes.MESSAGE_DECORATOR_UPDATE)
    public Decorator update(@ApiParam(name = "decorator id", required = true) @PathParam("decoratorId") final String decoratorId,
                            @ApiParam(name = "JSON body", required = true) DecoratorImpl decorator) throws NotFoundException {
        final Decorator originalDecorator = decoratorService.findById(decoratorId);
        checkPermission(RestPermissions.DECORATORS_CREATE);
        if (originalDecorator.stream().isPresent()) {
            checkPermission(RestPermissions.STREAMS_EDIT, originalDecorator.stream().get());
        }
        return this.decoratorService.save(decorator.toBuilder().id(originalDecorator.id()).build());
    }
}
