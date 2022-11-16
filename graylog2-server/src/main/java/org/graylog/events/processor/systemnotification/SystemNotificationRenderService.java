package org.graylog.events.processor.systemnotification;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Map;

import static org.apache.commons.lang.CharEncoding.UTF_8;

public class SystemNotificationRenderService {
    private static final Logger LOG = LoggerFactory.getLogger(SystemNotificationRenderService.class);
    private static final freemarker.template.Configuration cfg =
            new freemarker.template.Configuration(freemarker.template.Configuration.VERSION_2_3_28);

    @Inject
    public SystemNotificationRenderService() {
        cfg.setDefaultEncoding(UTF_8);
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setClassForTemplateLoading(SystemNotificationRenderService.class, "/org/graylog2/freemarker/templates/");
    }

    public String renderHtml(String notificationId, Map<String, Object> values) {
        try (Writer writer = new StringWriter()) {
            Template template = cfg.getTemplate(notificationId + "_html.ftl");
            template.process(values, writer);
            return writer.toString();
        } catch (TemplateException e) {
            throw new BadRequestException("Unable to render template " + notificationId + ": " + e.getMessage());
        } catch (IOException e) {
            throw new BadRequestException("Unable to locate template " + notificationId + ": " + e.getMessage());
        }
    }

    public String renderPlainText(String notificationId, Map<String, Object> values) {
        return null;
    }
}
