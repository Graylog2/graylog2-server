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
package org.graylog.events.processor.systemnotification;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;
import java.util.Map;

import static org.graylog2.shared.rest.documentation.generator.Generator.CLOUD_VISIBLE;

@Api(value = "System/Notification/Message", description = "Render system notification messages", tags = {CLOUD_VISIBLE})
@Path("/system/notification/message")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RequiresAuthentication
public class SystemNotificationRenderResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SystemNotificationRenderResource.class);

    private final SystemNotificationRenderService systemNotificationRenderService;

    @Inject
    public SystemNotificationRenderResource(SystemNotificationRenderService systemNotificationRenderService) {
        this.systemNotificationRenderService = systemNotificationRenderService;
    }

    @GET
    @Path("/html/{id}")
    @Produces(MediaType.TEXT_HTML)
    @ApiOperation(value = "Get HTML formatted message")
    public String renderHtml(@ApiParam(name = "id", required = true)
                             @PathParam("id") String id,
                             @ApiParam(name = "JSON body", required = false)
                             TemplateRenderRequest request) {
        Map<String, Object> values = (request != null) ? request.values() : new HashMap<>();
        return systemNotificationRenderService.renderHtml(id, values);
    }

    @GET
    @Path("/plaintext/{id}")
    @Produces(MediaType.TEXT_PLAIN)
    @ApiOperation(value = "Get plaintext formatted message")
    public String renderPlainText(@ApiParam(name = "id", required = true)
                                  @PathParam("id") String id,
                                  @ApiParam(name = "JSON body", required = false)
                                  TemplateRenderRequest request) {
        Map<String, Object> values = (request != null) ? request.values() : new HashMap<>();
        return systemNotificationRenderService.renderPlainText(id, values);
    }
}
