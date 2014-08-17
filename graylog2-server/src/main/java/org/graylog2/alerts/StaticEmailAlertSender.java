/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.alerts;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.graylog2.Configuration;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.users.User;
import org.graylog2.users.UserService;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.URI;
import java.util.List;


/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class StaticEmailAlertSender implements AlertSender {

    private static final Logger LOG = LoggerFactory.getLogger(StaticEmailAlertSender.class);

    private final StreamRuleService streamRuleService;
    protected final Configuration configuration;
    private final UserService userService;

    @Inject
    public StaticEmailAlertSender(Configuration configuration,
                                  StreamRuleService streamRuleService,
                                  UserService userService) {
        this.configuration = configuration;
        this.streamRuleService = streamRuleService;
        this.userService = userService;
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
        if(!configuration.isEmailTransportEnabled()) {
            throw new TransportConfigurationException("Email transport is not enabled!");
        }

        Email email = new SimpleEmail();
        email.setHostName(configuration.getEmailTransportHostname());
        email.setSmtpPort(configuration.getEmailTransportPort());
        if (configuration.isEmailTransportUseSsl()) {
            email.setSslSmtpPort(Integer.toString(configuration.getEmailTransportPort()));
        }

        if(configuration.isEmailTransportUseAuth()) {
            email.setAuthenticator(new DefaultAuthenticator(
                    configuration.getEmailTransportUsername(),
                    configuration.getEmailTransportPassword()
            ));
        }

        email.setSSLOnConnect(configuration.isEmailTransportUseSsl());
        email.setStartTLSEnabled(configuration.isEmailTransportUseTls());
        email.setFrom(configuration.getEmailTransportFromEmail());
        email.setSubject(buildSubject(stream, checkResult, configuration, backlog));

        StringBuilder body = new StringBuilder();
        body.append(buildBody(stream, checkResult, backlog));
        email.setMsg(body.toString());
        email.addTo(emailAddress);

        email.send();
    }

    protected String buildSubject(Stream stream, AlertCondition.CheckResult checkResult, Configuration config, List<Message> backlog) {
        StringBuilder sb = new StringBuilder();

        if (config.getEmailTransportSubjectPrefix() != null && !config.getEmailTransportSubjectPrefix().isEmpty()) {
            sb.append(config.getEmailTransportSubjectPrefix()).append(" ");
        }

        sb.append("Graylog2 alert for stream: ").append(stream.getTitle());

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
        if (configuration.getEmailTransportWebInterfaceUrl() != null)
            sb.append("Stream URL: ").append(
                    buildStreamDetailsURL(configuration.getEmailTransportWebInterfaceUrl(),
                            checkResult, stream));
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
        StringBuilder sb = new StringBuilder();

        int time = 5;
        if (checkResult.getTriggeredCondition().getParameters().get("time") != null)
            time = (int)checkResult.getTriggeredCondition().getParameters().get("time");

        DateTime dateAlertEnd = checkResult.getTriggeredAt();
        DateTime dateAlertStart = dateAlertEnd.minusMinutes(time);
        String alertStart = Tools.getISO8601String(dateAlertStart);
        String alertEnd = Tools.getISO8601String(dateAlertEnd);

        sb.append(baseUri).append("/streams/").append(stream.getId()).append("/messages");
        sb.append("?rangetype=absolute&from=").append(alertStart)
                .append("&to=").append(alertEnd).append("&q=*\n");

        return sb.toString();
    }

    protected String buildBacklogSummary(List<Message> backlog) {
        if (backlog == null || backlog.isEmpty())
            return "";

        StringBuilder sb = new StringBuilder();
        MessageFormatter messageFormatter = new MessageFormatter();

        sb.append("\n\nLast ");
        if (backlog.size() > 1)
            sb.append(backlog.size()).append(" relevant messages:\n");
        else
            sb.append("relevant message:\n");
        sb.append("======================\n\n");

        for (Message message : backlog) {
            sb.append(messageFormatter.formatForMail(message));
            sb.append("\n");
        }

        return sb.toString();
    }


    @Override
    public void sendEmails(Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) throws TransportConfigurationException, EmailException {
        if(!configuration.isEmailTransportEnabled()) {
            throw new TransportConfigurationException("Email transport is not enabled!");
        }

        if (stream.getAlertReceivers() == null || stream.getAlertReceivers().isEmpty()) {
            throw new RuntimeException("Stream [" + stream + "] has no alert receivers.");
        }

        // Send emails to subscribed users.
        if(stream.getAlertReceivers().get("users") != null) {
            for (String username : stream.getAlertReceivers().get("users")) {
                User user = userService.load(username);

                if(user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                    sendEmail(user.getEmail(), stream, checkResult, backlog);
                }
            }
        }

        // Send emails to directly subscribed email addresses.
        if(stream.getAlertReceivers().get("emails") != null) {
            for (String email : stream.getAlertReceivers().get("emails")) {
                if(!email.isEmpty()) {
                    sendEmail(email, stream, checkResult, backlog);
                }
            }
        }
    }
}