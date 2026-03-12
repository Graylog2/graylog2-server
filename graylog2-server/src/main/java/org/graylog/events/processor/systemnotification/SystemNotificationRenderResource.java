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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.apache.shiro.authz.annotation.RequiresAuthentication;
import org.graylog2.audit.jersey.NoAuditEvent;
import org.graylog2.notifications.Notification;
import org.graylog2.shared.rest.resources.RestResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.Map;

@Tag(name = "System/Notification/Message", description = "Render system notification messages")
@Path("/system/notification/message")
@Consumes(MediaType.APPLICATION_JSON)
@Produces({MediaType.TEXT_HTML, MediaType.TEXT_PLAIN})
@RequiresAuthentication
public class SystemNotificationRenderResource extends RestResource {
    private static final Logger LOG = LoggerFactory.getLogger(SystemNotificationRenderResource.class);

    private final SystemNotificationRenderService systemNotificationRenderService;

    @Inject
    public SystemNotificationRenderResource(SystemNotificationRenderService systemNotificationRenderService) {
        this.systemNotificationRenderService = systemNotificationRenderService;
    }

    @POST
    @NoAuditEvent("Doesn't change any data, only renders a notification message")
    @Path("/html/{type}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get HTML formatted message")
    public TemplateRenderResponse renderHtml(@Parameter(name = "type", required = true)
                                             @PathParam("type") Notification.Type type,
                                             @RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TemplateRenderRequest.class)))
                                             TemplateRenderRequest request) {
        return render(type, null, SystemNotificationRenderService.Format.HTML, request);
    }

    @POST
    @NoAuditEvent("Doesn't change any data, only renders a notification message")
    @Path("/html/{type}/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get HTML formatted message")
    public TemplateRenderResponse renderHtmlWithKey(@Parameter(name = "type", required = true) @PathParam("type") Notification.Type type,
                                                    @Parameter(name = "key", required = true) @PathParam("key") String key,
                                                    @RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TemplateRenderRequest.class)))
                                                    TemplateRenderRequest request) {
        return render(type, key, SystemNotificationRenderService.Format.HTML, request);
    }

    @POST
    @NoAuditEvent("Doesn't change any data, only renders a notification message")
    @Path("/plaintext/{type}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get plaintext formatted message")
    public TemplateRenderResponse renderPlainText(@Parameter(name = "type", required = true)
                                                  @PathParam("type") Notification.Type type,
                                                  @RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TemplateRenderRequest.class)))
                                                  TemplateRenderRequest request) {
        return render(type, null, SystemNotificationRenderService.Format.PLAINTEXT, request);
    }

    @POST
    @NoAuditEvent("Doesn't change any data, only renders a notification message")
    @Path("/plaintext/{type}/{key}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get plaintext formatted message")
    public TemplateRenderResponse renderPlainTextWithKey(@Parameter(name = "type", required = true) @PathParam("type") Notification.Type type,
                                                         @Parameter(name = "key", required = true) @PathParam("key") String key,
                                                         @RequestBody(content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TemplateRenderRequest.class)))
                                                         TemplateRenderRequest request) {
        return render(type, key, SystemNotificationRenderService.Format.PLAINTEXT, request);
    }

    private TemplateRenderResponse render(
            Notification.Type type,
            @Nullable String key,
            SystemNotificationRenderService.Format format,
            TemplateRenderRequest request) {
        Map<String, Object> values = (request != null) ? request.values() : new HashMap<>();
        SystemNotificationRenderService.RenderResponse renderResponse =
                systemNotificationRenderService.render(type, key, format, values);

        return TemplateRenderResponse.create(renderResponse.title, renderResponse.description);
    }
}
