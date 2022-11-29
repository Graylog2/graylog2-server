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

import freemarker.cache.ConditionalTemplateConfigurationFactory;
import freemarker.cache.PathGlobMatcher;
import freemarker.core.HTMLOutputFormat;
import freemarker.core.TemplateConfiguration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.apache.commons.lang.CharEncoding.UTF_8;

public class SystemNotificationRenderService {
    public enum Format {HTML, PLAINTEXT}

    private static final String KEY_NODE_ID = "node_id";
    private static final String KEY_TITLE = "_title";
    private static final String KEY_DESCRIPTION = "_description";
    private static final String KEY_CLOUD = "_cloud";
    public static final String TEMPLATE_BASE_PATH = "/org/graylog2/freemarker/templates/";
    private NotificationService notificationService;
    private org.graylog2.Configuration graylogConfig;
    private static final freemarker.template.Configuration cfg =
            new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_28);

    @Inject
    public SystemNotificationRenderService(NotificationService notificationService,
                                           org.graylog2.Configuration graylogConfig) {
        this.notificationService = notificationService;
        this.graylogConfig = graylogConfig;

        cfg.setDefaultEncoding(UTF_8);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setClassForTemplateLoading(SystemNotificationRenderService.class, TEMPLATE_BASE_PATH);

        TemplateConfiguration tcHTML = new TemplateConfiguration();
        tcHTML.setOutputFormat(HTMLOutputFormat.INSTANCE);
        cfg.setTemplateConfigurations(
                new ConditionalTemplateConfigurationFactory(
                        new PathGlobMatcher(Format.HTML.name() + "/**"),
                        tcHTML));
    }

/*
    public TemplateRenderResponse renderHtml(Notification notification) {
        return renderHtml(notification.getType(), notification.getDetails());
    }

    public TemplateRenderResponse renderHtml(Notification.Type type, Map<String, Object> values) {
        Notification notification = notificationService.getByType(type)
                .orElseThrow(() -> new IllegalArgumentException("Event type is not currently active"));

        // Add all data for template expansion into the details map
        if (values == null) {
            values = new HashMap<>();
        }
        if (notification.getDetails() != null) {
            values.putAll(notification.getDetails());
        }
        values.put(KEY_NODE_ID, notification.getNodeId());
        values.put(KEY_CLOUD, graylogConfig.isCloud());

        try (StringWriter writer = new StringWriter()) {
            Template template = cfg.getTemplate(type.toString().toLowerCase(Locale.ENGLISH) + ".ftl");

            values.put(KEY_TITLE, true);
            values.put(KEY_DESCRIPTION, false);
            template.process(values, writer);
            String title = writer.toString();

            writer.getBuffer().setLength(0);
            values.put(KEY_TITLE, false);
            values.put(KEY_DESCRIPTION, true);
            template.process(values, writer);
            String description = writer.toString();

            return TemplateRenderResponse.create(title, description);
        } catch (TemplateException e) {
            throw new BadRequestException("Unable to render template " + type.toString() + ": " + e.getMessage());
        } catch (IOException e) {
            throw new BadRequestException("Unable to locate template " + type.toString() + ": " + e.getMessage());
        }
    }

    public String renderPlainText(Notification notification) {
        return renderPlainText(notification.getType(), notification.getDetails());
    }

    public String renderPlainText(Notification.Type type, Map<String, Object> values) {
        TemplateRenderResponse templateRenderResponse = renderHtml(type, values);
        String plainTitle = new Source(templateRenderResponse.title()).getRenderer().toString();
        String plainDescription = new Source(templateRenderResponse.description()).getRenderer().toString();

        StringBuilder msg = new StringBuilder();
        msg.append("*** ").append(plainTitle.trim()).append(" ***");
        msg.append("\n\n").append(plainDescription.trim());
        return msg.toString();
    }
*/

    public RenderResponse render(Notification.Type type, Format format, Map<String, Object> values) {
        Notification notification = notificationService.getByType(type)
                .orElseThrow(() -> new IllegalArgumentException("Event type is not currently active"));
        return render(notification, format, values);
    }

    public RenderResponse render(Notification notification) {
        return render(notification, Format.PLAINTEXT, null);
    }

    public RenderResponse render(Notification notification, Format format, Map<String, Object> values) {
        // Add top level data for template expansion into the details map
        if (values == null) {
            values = new HashMap<>();
        }
        if (notification.getDetails() != null) {
            values.putAll(notification.getDetails());
        }
        values.put(KEY_NODE_ID, notification.getNodeId());
        values.put(KEY_CLOUD, graylogConfig.isCloud());

        final String templateRelPath = format.toString() + "/" + notification.getType().toString().toLowerCase(Locale.ENGLISH) + ".ftl";
        try (StringWriter writer = new StringWriter()) {
            Template template = cfg.getTemplate(templateRelPath);

            values.put(KEY_TITLE, true);
            values.put(KEY_DESCRIPTION, false);
            template.process(values, writer);
            String title = writer.toString();

            writer.getBuffer().setLength(0);
            values.put(KEY_TITLE, false);
            values.put(KEY_DESCRIPTION, true);
            template.process(values, writer);
            String description = writer.toString();

            return new RenderResponse(title, description);
        } catch (TemplateException e) {
            throw new BadRequestException("Unable to render template " + notification.getType().toString() + ": " + e.getMessage());
        } catch (IOException e) {
            throw new BadRequestException("Unable to locate template " + notification.getType().toString() + ": " + e.getMessage());
        }
    }

    public class RenderResponse {
        public String title;
        public String description;
        public RenderResponse(String title, String description) {
            this.title = title;
            this.description = description;
        }
    }
}
