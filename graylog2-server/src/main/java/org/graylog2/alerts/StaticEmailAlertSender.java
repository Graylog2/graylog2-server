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

import com.google.common.collect.Lists;
import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.graylog2.configuration.EmailConfiguration;
import org.graylog2.database.NotFoundException;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.database.users.User;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.users.UserService;
import org.graylog2.streams.StreamRuleService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

public class StaticEmailAlertSender implements AlertSender {

    private static final Logger LOG = LoggerFactory.getLogger(StaticEmailAlertSender.class);

    private final StreamRuleService streamRuleService;
    protected final EmailConfiguration configuration;
    private final UserService userService;
    private final NotificationService notificationService;
    private final NodeId nodeId;

    @Inject
    public StaticEmailAlertSender(EmailConfiguration configuration,
                                  StreamRuleService streamRuleService,
                                  UserService userService,
                                  NotificationService notificationService,
                                  NodeId nodeId) {
        this.configuration = configuration;
        this.streamRuleService = streamRuleService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.nodeId = nodeId;
    }

    @Override
    public void initialize(org.graylog2.plugin.configuration.Configuration configuration) {
    }

    @Override
    public void sendEmails(Stream stream, AlertCondition.CheckResult checkResult) throws TransportConfigurationException, EmailException {
        sendEmails(stream, checkResult, null);
    }

    private void sendEmail(String emailAddress, Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) throws TransportConfigurationException, EmailException {
        LOG.debug("Sending mail to " + emailAddress);
        if(!configuration.isEnabled()) {
            throw new TransportConfigurationException("Email transport is not enabled in server configuration file!");
        }

        final Email email = new SimpleEmail();
        email.setHostName(configuration.getHostname());
        email.setSmtpPort(configuration.getPort());
        if (configuration.isUseSsl()) {
            email.setSslSmtpPort(Integer.toString(configuration.getPort()));
        }

        if(configuration.isUseAuth()) {
            email.setAuthenticator(new DefaultAuthenticator(
                    configuration.getUsername(),
                    configuration.getPassword()
            ));
        }

        email.setSSLOnConnect(configuration.isUseSsl());
        email.setStartTLSEnabled(configuration.isUseTls());
        email.setFrom(configuration.getFromEmail());
        email.setSubject(buildSubject(stream, checkResult, backlog));
        email.setMsg(buildBody(stream, checkResult, backlog));
        email.addTo(emailAddress);

        email.send();
    }

    protected String buildSubject(Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) {
        StringBuilder sb = new StringBuilder();

        final String subjectPrefix = configuration.getSubjectPrefix();
        if (!isNullOrEmpty(subjectPrefix)) {
            sb.append(subjectPrefix).append(" ");
        }

        sb.append("Graylog alert for stream: ").append(stream.getTitle());

        return sb.toString();
    }

    protected String buildBody(Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) {
        StringBuilder sb = new StringBuilder();

        sb.append(checkResult.getResultDescription());

        sb.append("\n\n");
        sb.append("##########\n");
        sb.append("Date: ").append(Tools.iso8601().toString()).append("\n");
        sb.append("Stream ID: ").append(stream.getId()).append("\n");
        sb.append("Stream title: ").append(stream.getTitle()).append("\n");
        sb.append("Stream URL: ").append(buildStreamDetailsURL(configuration.getWebInterfaceUri(), checkResult, stream)).append("\n");

        try {
            sb.append("Stream rules: ").append(streamRuleService.loadForStream(stream)).append("\n");
        } catch (NotFoundException e) {
            LOG.error("Unable to find stream rules for stream: " + stream.getId(), e);
        }
        sb.append("Alert triggered at: ").append(checkResult.getTriggeredAt()).append("\n");
        sb.append("Triggered condition: ").append(checkResult.getTriggeredCondition()).append("\n");
        sb.append("##########");

        if (backlog != null) {
            sb.append(buildBacklogSummary(backlog));
        }

        return sb.toString();
    }

    protected String buildStreamDetailsURL(URI baseUri, AlertCondition.CheckResult checkResult, Stream stream) {
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

    protected String buildBacklogSummary(List<Message> backlog) {
        if (backlog == null || backlog.isEmpty())
            return "";

        final StringBuilder sb = new StringBuilder();
        MessageFormatter messageFormatter = new MessageFormatter();

        sb.append("\n\nLast ");
        if (backlog.size() > 1)
            sb.append(backlog.size()).append(" relevant messages:\n");
        else
            sb.append("relevant message:\n");
        sb.append("======================\n\n");

        for (final Message message : backlog) {
            sb.append(messageFormatter.formatForMail(message));
            sb.append("\n");
        }

        return sb.toString();
    }


    @Override
    public void sendEmails(Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) throws TransportConfigurationException, EmailException {
        if(!configuration.isEnabled()) {
            throw new TransportConfigurationException("Email transport is not enabled in server configuration file!");
        }

        if (stream.getAlertReceivers() == null || stream.getAlertReceivers().isEmpty()) {
            throw new RuntimeException("Stream [" + stream + "] has no alert receivers.");
        }

        final List<String> recipients = Lists.newArrayList();

        // Send emails to subscribed users.
        if(stream.getAlertReceivers().get("users") != null) {
            for (String username : stream.getAlertReceivers().get("users")) {
                final User user = userService.load(username);

                if(user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                    recipients.add(user.getEmail());
                }
            }
        }

        // Send emails to directly subscribed email addresses.
        if(stream.getAlertReceivers().get("emails") != null) {
            for (String email : stream.getAlertReceivers().get("emails")) {
                if(!email.isEmpty()) {
                    recipients.add(email);
                }
            }
        }

        if (recipients.size() == 0) {
            final Notification notification = notificationService.buildNow()
                    .addNode(nodeId.toString())
                    .addType(Notification.Type.GENERIC)
                    .addSeverity(Notification.Severity.NORMAL)
                    .addDetail("title", "Stream \"" + stream.getTitle() + "\" is alerted, but no recipients have been defined!")
                    .addDetail("description", "To fix this, go to the alerting configuration of the stream and add at least one alert recipient.");
            notificationService.publishIfFirst(notification);
        }

        for (String email : recipients)
            sendEmail(email, stream, checkResult, backlog);
    }
}
