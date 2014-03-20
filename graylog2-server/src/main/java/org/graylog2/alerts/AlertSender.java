/*
 * Copyright 2013-2014 TORCH GmbH
 *
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
import org.graylog2.Core;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.StreamRuleService;
import org.graylog2.streams.StreamRuleServiceImpl;
import org.graylog2.users.User;
import org.graylog2.users.UserService;
import org.graylog2.users.UserServiceImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class AlertSender {

    private static final Logger LOG = LoggerFactory.getLogger(AlertSender.class);

    private final Core core;
    private final StreamRuleService streamRuleService;
    private final Configuration configuration;
    private final UserService userService;

    public AlertSender(Core core) {
        this.core = core;
        this.streamRuleService = new StreamRuleServiceImpl(core.getMongoConnection());
        this.configuration = core.getConfiguration();
        this.userService = new UserServiceImpl(core.getMongoConnection(), core.getConfiguration());
    }

    public void sendEmails(Stream stream, AlertCondition.CheckResult checkResult) throws TransportConfigurationException, EmailException {
        sendEmails(stream, checkResult, null);
    }

    private void sendEmail(String emailAddress, Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) throws TransportConfigurationException, EmailException {
        if(!configuration.isEmailTransportEnabled()) {
            throw new TransportConfigurationException();
        }

        Email email = new SimpleEmail();
        email.setHostName(configuration.getEmailTransportHostname());
        email.setSmtpPort(configuration.getEmailTransportPort());
        if (configuration.isEmailTransportUseSsl()) {
            email.setSslSmtpPort(Integer.toString(configuration.getEmailTransportPort()));
        }

        if(core.getConfiguration().isEmailTransportUseAuth()) {
            email.setAuthenticator(new DefaultAuthenticator(
                    configuration.getEmailTransportUsername(),
                    configuration.getEmailTransportPassword()
            ));
        }

        email.setSSLOnConnect(configuration.isEmailTransportUseSsl());
        email.setStartTLSEnabled(configuration.isEmailTransportUseTls());
        email.setFrom(configuration.getEmailTransportFromEmail());
        email.setSubject(buildSubject(stream, checkResult, configuration));

        StringBuilder body = new StringBuilder();
        body.append(buildBody(stream, checkResult));
        if (backlog != null) {
            body.append(buildBacklogSummary(backlog));
        }
        email.setMsg(body.toString());
        email.addTo(emailAddress);

        email.send();
    }

    private String buildSubject(Stream stream, AlertCondition.CheckResult checkResult, Configuration config) {
        StringBuilder sb = new StringBuilder();

        if (config.getEmailTransportSubjectPrefix() != null && !config.getEmailTransportSubjectPrefix().isEmpty()) {
            sb.append(config.getEmailTransportSubjectPrefix()).append(" ");
        }

        sb.append("Graylog2 alert for stream: ").append(stream.getTitle());

        return sb.toString();
    }

    private String buildBody(Stream stream, AlertCondition.CheckResult checkResult) {
        StringBuilder sb = new StringBuilder();

        sb.append(checkResult.getResultDescription());

        sb.append("\n\n");
        sb.append("##########\n");
        sb.append("Date: ").append(Tools.iso8601().toString()).append("\n");
        sb.append("Stream ID: ").append(stream.getId()).append("\n");
        sb.append("Stream title: ").append(stream.getTitle()).append("\n");
        try {
            sb.append("Stream rules: ").append(streamRuleService.loadForStream(stream)).append("\n");
        } catch (NotFoundException e) {
            LOG.error("Unable to find stream rules for stream: " + stream.getId(), e);
        }
        sb.append("Alert triggered at: ").append(checkResult.getTriggeredAt()).append("\n");
        sb.append("Triggered condition: ").append(checkResult.getTriggeredCondition()).append("\n");
        sb.append("##########");

        return sb.toString();
    }

    private String buildBacklogSummary(List<Message> backlog) {
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


    public void sendEmails(Stream stream, AlertCondition.CheckResult checkResult, List<Message> backlog) throws TransportConfigurationException, EmailException {
        if(!configuration.isEmailTransportEnabled()) {
            throw new TransportConfigurationException();
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