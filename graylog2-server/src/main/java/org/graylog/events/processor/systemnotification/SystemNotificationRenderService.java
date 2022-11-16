package org.graylog.events.processor.systemnotification;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.BadRequestException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
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
    }

    public String renderHtml(String notificationId, Map<String, Object> values) {
        String templateName = notificationId + "_html";
        try (Reader reader = new InputStreamReader(
                getClass().getClassLoader().getResourceAsStream(templateName))) {
            Template template = new Template(notificationId, reader, cfg);
            Writer writer = new StringWriter();
            template.process(values, writer);
            return template.toString();
        } catch (TemplateException e) {
            throw new BadRequestException("Unable to render template " + templateName + ": " + e.getMessage());
        } catch (IOException e) {
            throw new BadRequestException("Unable to locate template " + templateName + ": " + e.getMessage());
        }
    }

    public String renderPlainText(String notificationId, Map<String, Object> values) {
        return null;
    }
}
