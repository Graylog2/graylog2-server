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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.apache.shiro.authz.annotation.RequiresPermissions;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNDescriptorService;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.annotation.Nullable;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Api(value = "Authorization/GrantsOverview", description = "Grants overview")
@Path("/authz/grants-overview")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class GrantsOverviewResource extends RestResource {
    private final DBGrantService grantService;
    private final GRNDescriptorService descriptorService;

    @Inject
    public GrantsOverviewResource(DBGrantService grantService,
                                  GRNDescriptorService descriptorService) {
        this.grantService = grantService;
        this.descriptorService = descriptorService;
    }

    @GET
    @ApiOperation("Return an overview of all grants in the system")
    @RequiresPermissions(RestPermissions.GRANTS_OVERVIEW_READ)
    public Response getOverview() {
        final List<GrantSummary> grants = grantService.getAll().stream()
                .map(grant -> GrantSummary.of(grant, descriptorService))
                .collect(Collectors.toList());

        return Response.ok(Collections.singletonMap("grants", grants)).build();
    }

    @AutoValue
    public static abstract class GrantSummary {
        @JsonProperty("id")
        public abstract String id();

        @JsonProperty("grantee")
        public abstract GRN grantee();

        @JsonProperty("grantee_title")
        public abstract String granteeTitle();

        @JsonProperty("capability")
        public abstract Capability capability();

        @JsonProperty("target")
        public abstract GRN target();

        @JsonProperty("target_title")
        public abstract String targetTitle();

        @JsonProperty("created_by")
        public abstract String createdBy();

        @JsonProperty("created_at")
        public abstract ZonedDateTime createdAt();

        @JsonProperty("updated_by")
        public abstract String updatedBy();

        @JsonProperty("updated_at")
        public abstract ZonedDateTime updatedAt();

        @JsonProperty("expires_at")
        public abstract Optional<ZonedDateTime> expiresAt();

        public static GrantSummary of(GrantDTO grant, GRNDescriptorService descriptorService) {
            return builder()
                    .id(grant.id())
                    .grantee(grant.grantee())
                    .granteeTitle(grant.grantee().type() + ": " + descriptorService.getDescriptor(grant.grantee()).title())
                    .capability(grant.capability())
                    .target(grant.target())
                    .targetTitle(grant.target().type() + ": " + descriptorService.getDescriptor(grant.target()).title())
                    .createdBy(grant.createdBy())
                    .createdAt(grant.createdAt())
                    .updatedBy(grant.updatedBy())
                    .updatedAt(grant.updatedAt())
                    .expiresAt(grant.expiresAt().orElse(null))
                    .build();
        }

        public static Builder builder() {
            return new AutoValue_GrantsOverviewResource_GrantSummary.Builder();
        }

        @AutoValue.Builder
        public abstract static class Builder {
            public abstract Builder id(String id);

            public abstract Builder grantee(GRN grantee);

            public abstract Builder granteeTitle(String granteeTitle);

            public abstract Builder capability(Capability capability);

            public abstract Builder target(GRN target);

            public abstract Builder targetTitle(String targetTitle);

            public abstract Builder createdBy(String createdBy);

            public abstract Builder createdAt(ZonedDateTime createdAt);

            public abstract Builder updatedBy(String updatedBy);

            public abstract Builder updatedAt(ZonedDateTime updatedAt);

            public abstract Builder expiresAt(@Nullable ZonedDateTime expiresAt);

            public abstract GrantSummary build();
        }
    }
}
