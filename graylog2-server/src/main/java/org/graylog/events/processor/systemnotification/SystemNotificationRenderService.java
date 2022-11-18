package org.graylog.events.processor.systemnotification;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import net.htmlparser.jericho.Source;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import static org.apache.commons.lang.CharEncoding.UTF_8;

public class SystemNotificationRenderService {
    static final String KEY_TITLE = "_title";
    static final String KEY_DESCRIPTION = "_description";
    static final String KEY_CLOUD = "_cloud";
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
        cfg.setClassForTemplateLoading(SystemNotificationRenderService.class, "/org/graylog2/freemarker/templates/");
    }

    public TemplateRenderResponse renderHtml(Notification notification) {
        return renderHtml(notification.getType(), notification.getDetails());
    }

    public TemplateRenderResponse renderHtml(Notification.Type type, Map<String, Object> values) {
        Notification notification = notificationService.getByType(type)
                .orElseThrow(() -> new IllegalArgumentException("Event type is not currently active"));
        values.putAll(notification.getDetails());
        values.put(KEY_CLOUD, graylogConfig.isCloud());

        try (StringWriter writer = new StringWriter()) {
            Template template = cfg.getTemplate(type.toString().toLowerCase() + ".ftl");

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

        return plainTitle + "\n" + plainDescription;
    }
}
