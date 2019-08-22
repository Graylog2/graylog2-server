/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.alerts;

import com.floreysoft.jmte.Engine;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Strings;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailConstants;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.graylog2.email.configuration.EmailConfiguration;
import org.graylog2.email.configuration.EmailConfigurationService;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

public class FormattedEmailAlertSender implements AlertSender {
    private static final Logger LOG = LoggerFactory.getLogger(FormattedEmailAlertSender.class);

    public static final String bodyTemplate = "##########\n" +
            "Alert Description: ${check_result.resultDescription}\n" +
            "Date: ${check_result.triggeredAt}\n" +
            "Stream ID: ${stream.id}\n" +
            "Stream title: ${stream.title}\n" +
            "Stream description: ${stream.description}\n" +
            "Alert Condition Title: ${alertCondition.title}\n" +
            "${if stream_url}Stream URL: ${stream_url}${end}\n" +
            "\n" +
            "Triggered condition: ${check_result.triggeredCondition}\n" +
            "##########\n\n" +
            "${if backlog}" +
            "Last messages accounting for this alert:\n" +
            "${foreach backlog message}" +
            "${message}\n\n" +
            "${end}" +
            "${else}" +
            "<No backlog>\n" +
            "${end}" +
            "\n";

    private final EmailConfigurationService emailConfigurationService;
    private final Engine templateEngine;
    private final NotificationService notificationService;
    private final NodeId nodeId;
    private Configuration pluginConfig;

    @Inject
    public FormattedEmailAlertSender(EmailConfigurationService emailConfigurationService,
                                     NotificationService notificationService,
                                     NodeId nodeId,
                                     Engine templateEngine) {
        this.emailConfigurationService = requireNonNull(emailConfigurationService, "emailConfigurationService");
        this.notificationService = requireNonNull(notificationService, "notificationService");
        this.nodeId = requireNonNull(nodeId, "nodeId");
        this.templateEngine = requireNonNull(templateEngine, "templateEngine");
    }

    @Override
    public void initialize(Configuration configuration) {
        this.pluginConfig = configuration;
    }

    @VisibleForTesting
    String buildSubject(Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) {
        final String template;
        if (pluginConfig == null || pluginConfig.getString("subject") == null) {
            template = "Graylog alert for stream: ${stream.title}: ${check_result.resultDescription}";
        } else {
            template = pluginConfig.getString("subject");
        }

        Map<String, Object> model = getModel(stream, checkResult, backlog);

        return templateEngine.transform(template, model);
    }

    @VisibleForTesting
    String buildBody(Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) {
        final String template;
        if (pluginConfig == null || pluginConfig.getString("body") == null) {
            template = bodyTemplate;
        } else {
            template = pluginConfig.getString("body");
        }
        Map<String, Object> model = getModel(stream, checkResult, backlog);

        return this.templateEngine.transform(template, model);
    }

    private Map<String, Object> getModel(Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) {
        final EmailConfiguration emailConfiguration = emailConfigurationService.load();
        Map<String, Object> model = new HashMap<>();
        model.put("stream", stream);
        model.put("check_result", checkResult);
        model.put("stream_url", buildStreamDetailsURL(emailConfiguration.webInterfaceUri(), checkResult, stream));
        model.put("alertCondition", checkResult.getTriggeredCondition());

        final List<Message> messages = firstNonNull(backlog, Collections.<Message>emptyList());
        model.put("backlog", messages);
        model.put("backlog_size", messages.size());

        return model;
    }

    private String buildStreamDetailsURL(URI baseUri, AlertCondition.CheckResult checkResult, Stream stream) {
        // Return an informational message if the web interface URL hasn't been set
        if (baseUri == null || isNullOrEmpty(baseUri.getHost())) {
            return "Please configure 'transport_email_web_interface_url' in your Graylog configuration file.";
        }

        int time = 5;
        if (checkResult.getTriggeredCondition().getParameters().get("time") != null) {
            time = (int) checkResult.getTriggeredCondition().getParameters().get("time");
        }

        DateTime dateAlertEnd = checkResult.getTriggeredAt();
        DateTime dateAlertStart = dateAlertEnd.minusMinutes(time);
        String alertStart = Tools.getISO8601String(dateAlertStart);
        String alertEnd = Tools.getISO8601String(dateAlertEnd);

        return baseUri + "/streams/" + stream.getId() + "/messages?rangetype=absolute&from=" + alertStart + "&to=" + alertEnd + "&q=*";
    }

    @Override
    public void sendEmails(Stream stream, EmailRecipients recipients, AlertCondition.CheckResult checkResult) throws TransportConfigurationException, EmailException {
        sendEmails(stream, recipients, checkResult, null);
    }

    private void sendEmail(EmailConfiguration emailConfiguration, String emailAddress, Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) throws TransportConfigurationException, EmailException {
        LOG.debug("Sending mail to " + emailAddress);
        if(!emailConfiguration.enabled()) {
            throw new TransportConfigurationException("Email transport is not enabled!");
        }

        final Email email = new SimpleEmail();
        email.setCharset(EmailConstants.UTF_8);

        if (isNullOrEmpty(emailConfiguration.hostname())) {
            throw new TransportConfigurationException("No hostname configured for email transport while trying to send alert email!");
        } else {
            email.setHostName(emailConfiguration.hostname());
        }
        email.setSmtpPort(emailConfiguration.port());
        if (emailConfiguration.useSsl()) {
            email.setSslSmtpPort(Integer.toString(emailConfiguration.port()));
        }

        if(emailConfiguration.useAuth()) {
            email.setAuthenticator(new DefaultAuthenticator(
                Strings.nullToEmpty(emailConfiguration.username()),
                Strings.nullToEmpty(emailConfiguration.password())
            ));
        }

        email.setSSLOnConnect(emailConfiguration.useSsl());
        email.setStartTLSEnabled(emailConfiguration.useTls());
        if (pluginConfig != null && !isNullOrEmpty(pluginConfig.getString("sender"))) {
            email.setFrom(pluginConfig.getString("sender"));
        } else {
            email.setFrom(emailConfiguration.fromEmail());
        }
        email.setSubject(buildSubject(stream, checkResult, backlog));
        email.setMsg(buildBody(stream, checkResult, backlog));
        email.addTo(emailAddress);

        email.send();
    }

    @Override
    public void sendEmails(Stream stream, EmailRecipients recipients, AlertCondition.CheckResult checkResult, List<Message> backlog) throws TransportConfigurationException, EmailException {
        final EmailConfiguration emailConfiguration = emailConfigurationService.load();
        if(!emailConfiguration.enabled()) {
            throw new TransportConfigurationException("Email transport is not enabled!");
        }

        if (recipients == null || recipients.isEmpty()) {
            throw new RuntimeException("Cannot send emails: empty recipient list.");
        }

        final Set<String> recipientsSet = recipients.getEmailRecipients();
        if (recipientsSet.size() == 0) {
            final Notification notification = notificationService.buildNow()
                .addNode(nodeId.toString())
                .addType(Notification.Type.GENERIC)
                .addSeverity(Notification.Severity.NORMAL)
                .addDetail("title", "Stream \"" + stream.getTitle() + "\" is alerted, but no recipients have been defined!")
                .addDetail("description", "To fix this, go to the alerting configuration of the stream and add at least one alert recipient.");
            notificationService.publishIfFirst(notification);
        }

        for (String email : recipientsSet) {
            sendEmail(emailConfiguration, email, stream, checkResult, backlog);
        }
    }
}


