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
package org.graylog2.bootstrap.preflight.web.resources;

import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.bootstrap.preflight.PreflightConstants;
import org.graylog2.plugin.certificates.RenewalPolicy;
import org.graylog2.plugin.cluster.ClusterConfigService;

import jakarta.inject.Inject;

import jakarta.validation.constraints.NotNull;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path(PreflightConstants.API_PREFIX + "renewal_policy")
@Produces(MediaType.APPLICATION_JSON)
public class CertificateRenewalPolicyResource {
    private final ClusterConfigService clusterConfigService;

    @Inject
    public CertificateRenewalPolicyResource(final ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @GET
    public RenewalPolicy get() {
        return this.clusterConfigService.get(RenewalPolicy.class);
    }

    @POST
    @NoAuditEvent("No Audit Event needed")
    public void set(@NotNull RenewalPolicy renewalPolicy) {
        this.clusterConfigService.write(renewalPolicy);
    }
}
