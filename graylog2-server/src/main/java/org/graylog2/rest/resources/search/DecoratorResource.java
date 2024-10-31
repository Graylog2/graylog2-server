/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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

import jakarta.inject.Inject;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@RequiresAuthentication
@Api(value = "Search/Decorators", description = "Message search decorators", tags = {CLOUD_VISIBLE})
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
    @ApiOperation(value = "Returns all configured message decorations")
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
    public Decorator create(@ApiParam(name = "JSON body", required = true) @Valid @NotNull DecoratorImpl decorator) {
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
    public Decorator update(@ApiParam(name = "decorator id", required = true)
                            @PathParam("decoratorId") final String decoratorId,
                            @ApiParam(name = "JSON body", required = true)
                            @Valid @NotNull DecoratorImpl decorator) throws NotFoundException {
        final Decorator originalDecorator = decoratorService.findById(decoratorId);
        checkPermission(RestPermissions.DECORATORS_CREATE);
        if (originalDecorator.stream().isPresent()) {
            checkPermission(RestPermissions.STREAMS_EDIT, originalDecorator.stream().get());
        }
        return this.decoratorService.save(decorator.toBuilder().id(originalDecorator.id()).build());
    }
}
