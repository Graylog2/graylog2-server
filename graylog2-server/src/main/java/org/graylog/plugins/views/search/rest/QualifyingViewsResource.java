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
package org.graylog.plugins.views.search.rest;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog.plugins.views.search.views.QualifyingViewsService;
import org.graylog.plugins.views.search.views.ViewDTO;
import org.graylog.plugins.views.search.views.ViewParameterSummaryDTO;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.plugin.rest.PluginRestResource;
import org.graylog2.shared.rest.resources.RestResource;
import org.graylog2.shared.security.RestPermissions;

import javax.inject.Inject;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Collection;
import java.util.stream.Collectors;

@Api(value = "Views/QualifyingViews")
@Path("/views/forValue")
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class QualifyingViewsResource extends RestResource implements PluginRestResource {
    private final QualifyingViewsService qualifyingViewsService;

    @Inject
    public QualifyingViewsResource(QualifyingViewsService qualifyingViewsService) {
        this.qualifyingViewsService = qualifyingViewsService;
    }

    @POST
    @ApiOperation("Get all views that match given parameter value")
    @NoAuditEvent("Only returning matching views, not changing any data")
    public Collection<ViewParameterSummaryDTO> forParameter() {
        return qualifyingViewsService.forValue()
                .stream()
                .filter(view -> isPermitted(ViewsRestPermissions.VIEW_READ, view.id())
                        || (view.type().equals(ViewDTO.Type.DASHBOARD) && isPermitted(RestPermissions.DASHBOARDS_READ, view.id())))
                .collect(Collectors.toSet());
    }
}
