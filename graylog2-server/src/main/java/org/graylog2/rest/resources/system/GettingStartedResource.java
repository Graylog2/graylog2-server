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
package org.graylog2.rest.resources.system;

import com.google.common.collect.Sets;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.AuditEventTypes;
import org.graylog2.audit.jersey.AuditEvent;
import org.graylog2.gettingstarted.GettingStartedState;
import org.graylog2.plugin.Version;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.rest.models.system.DisplayGettingStarted;
import org.graylog2.shared.rest.resources.RestResource;

import javax.inject.Inject;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Locale;

@RequiresAuthentication
@Api(value = "System/GettingStartedGuides", description = "Getting Started guide")
@Path("/system/gettingstarted")
@Produces(MediaType.APPLICATION_JSON)
public class GettingStartedResource extends RestResource {

    private final ClusterConfigService clusterConfigService;

    @Inject
    public GettingStartedResource(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    @GET
    @ApiOperation("Check whether to display the Getting started guide for this version")
    public DisplayGettingStarted displayGettingStarted() {
        final GettingStartedState gettingStartedState = clusterConfigService.get(GettingStartedState.class);
        if (gettingStartedState == null) {
            return  DisplayGettingStarted.create(true);
        }
        final boolean isDismissed = gettingStartedState.dismissedInVersions().contains(currentMinorVersionString());
        return DisplayGettingStarted.create(!isDismissed);
    }

    @POST
    @Path("dismiss")
    @ApiOperation("Dismiss auto-showing getting started guide for this version")
    @AuditEvent(type = AuditEventTypes.GETTING_STARTED_GUIDE_OPT_OUT_CREATE)
    public void dismissGettingStarted() {
        final GettingStartedState gettingStartedState = clusterConfigService.getOrDefault(GettingStartedState.class,
                                                                                GettingStartedState.create(Sets.<String>newHashSet()));
        gettingStartedState.dismissedInVersions().add(currentMinorVersionString());
        clusterConfigService.write(gettingStartedState);

    }

    private static String currentMinorVersionString() {
        return String.format(Locale.ENGLISH, "%d.%d",
                             Version.CURRENT_CLASSPATH.getVersion().getMajorVersion(),
                             Version.CURRENT_CLASSPATH.getVersion().getMinorVersion());
    }
}
