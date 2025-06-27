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
package org.graylog.security.rest;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import jakarta.inject.Inject;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNRegistry;
import org.graylog.security.Capability;
import org.graylog.security.CapabilityDescriptor;
import org.graylog.security.CapabilityRegistry;
import org.graylog2.plugin.security.Permission;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "Authorization/Capabilities", description = "Capabilities overview", tags = {CLOUD_VISIBLE})
@Path("/authz/capabilities")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class CapabilitiesResource extends RestResource {
    private final CapabilityRegistry capabilityRegistry;
    private final GRNRegistry grnRegistry;

    @Inject
    public CapabilitiesResource(CapabilityRegistry capabilityRegistry, GRNRegistry grnRegistry) {
        this.capabilityRegistry = capabilityRegistry;
        this.grnRegistry = grnRegistry;
    }

    @GET
    @ApiOperation("Return a list of all capabilities in the system")
    public CapabilitiesResponse list() {
        return new CapabilitiesResponse(
                capabilityRegistry.allSharingCapabilities()
                        .stream()
                        .filter(descriptor -> isPermitted(RestPermissions.CAPABILITIES_READ, descriptor.capability().name()))
                        .map(CapabilityDescriptorResponse::new)
                        .toList()
        );
    }

    @GET
    @Path("/{capability}")
    @ApiOperation("Return the requested capability")
    public CapabilityDescriptorResponse get(@ApiParam("capability") @PathParam("capability") @NotBlank String capabilityString) {

        try {
            final var capability = Capability.valueOf(capabilityString.toUpperCase(Locale.ROOT));

            checkPermission(RestPermissions.CAPABILITIES_READ, capability.name());

            return capabilityRegistry.allSharingCapabilities()
                    .stream()
                    .filter(descriptor -> descriptor.capability() == capability)
                    .map(CapabilityDescriptorResponse::new)
                    .findFirst()
                    .orElse(null);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    public record CapabilitiesResponse(@JsonProperty("capabilities") List<CapabilityDescriptorResponse> capabilities) {
    }

    public record CapabilityDescriptorResponse(@JsonIgnore CapabilityDescriptor descriptor) {
        @JsonProperty
        public String title() {
            return descriptor.title();
        }

        @JsonProperty
        public Capability capability() {
            return descriptor.capability();
        }

        @JsonProperty
        public Map<String, List<String>> permissions() {
            return descriptor.permissions()
                    .asMap()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(
                            grnTypeCollectionEntry -> grnTypeCollectionEntry.getKey().type(),
                            entry -> entry.getValue().stream()
                                    .map(Permission::permission)
                                    .sorted()
                                    .collect(Collectors.toList())
                    ));

        }
    }

    @GET
    @Path("/{capability}/{entity}")
    @ApiOperation("Return a list of all capabilities in the system")
    public CapabilityPermissionsResponse entityCapabilityPermissions(@ApiParam("capability") @PathParam("capability") @NotBlank String capabilityString,
                                                                     @ApiParam("entity") @PathParam("entity") @NotBlank String entity) {
        try {
            final var entityGrn = grnRegistry.parse(entity);
            final var capability = Capability.valueOf(capabilityString.toUpperCase(Locale.ROOT));

            checkPermission(RestPermissions.CAPABILITIES_READ, capability.name());

            final var permissions = capabilityRegistry.getPermissions(capability, entityGrn.grnType())
                    .stream()
                    .map(p -> p.toShiroPermission(entityGrn).toString())
                    .sorted()
                    .toList();

            return new CapabilityPermissionsResponse(capability, entityGrn, permissions);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage(), e);
        }
    }

    public record CapabilityPermissionsResponse(@JsonProperty("capability") Capability capability,
                                                @JsonProperty("entity") GRN entity,
                                                @JsonProperty("permissions") List<String> permissions) {

    }
}
