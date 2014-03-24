/**
 * Copyright 2014 Lennart Koopmann <lennart@torch.sh>
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
 *
 */
package org.graylog2.alerts;

import org.apache.commons.mail.DefaultAuthenticator;
import org.apache.commons.mail.Email;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.SimpleEmail;
import org.graylog2.Configuration;
import org.graylog2.Core;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.streams.StreamImpl;
import org.graylog2.users.User;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;


/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class AlertSender {

    private static final Logger LOG = LoggerFactory.getLogger(AlertSender.class);

    private final Core core;

    public AlertSender(Core core) {
        this.core = core;
    }

    public void sendEmails(StreamImpl stream, AlertCondition.CheckResult checkResult) throws TransportConfigurationException, EmailException {
        sendEmails(stream, checkResult, null);
    }

    private void sendEmail(String emailAddress, StreamImpl stream, AlertCondition.CheckResult checkResult, List<Message> backlog) throws TransportConfigurationException, EmailException {
        if(!core.getConfiguration().isEmailTransportEnabled()) {
            throw new TransportConfigurationException();
        }

        Email email = new SimpleEmail();
        email.setHostName(core.getConfiguration().getEmailTransportHostname());
        email.setSmtpPort(core.getConfiguration().getEmailTransportPort());
        if (core.getConfiguration().isEmailTransportUseSsl()) {
            email.setSslSmtpPort(Integer.toString(core.getConfiguration().getEmailTransportPort()));
        }

        if(core.getConfiguration().isEmailTransportUseAuth()) {
            email.setAuthenticator(new DefaultAuthenticator(
                    core.getConfiguration().getEmailTransportUsername(),
                    core.getConfiguration().getEmailTransportPassword()
            ));
        }

        email.setSSLOnConnect(core.getConfiguration().isEmailTransportUseSsl());
        email.setStartTLSEnabled(core.getConfiguration().isEmailTransportUseTls());
        email.setFrom(core.getConfiguration().getEmailTransportFromEmail());
        email.setSubject(buildSubject(stream, checkResult, core.getConfiguration()));

        StringBuilder body = new StringBuilder();
        body.append(buildBody(stream, checkResult));
        if (backlog != null) {
            body.append(buildBacklogSummary(backlog));
        }
        email.setMsg(body.toString());
        email.addTo(emailAddress);

        email.send();
    }

    private String buildSubject(StreamImpl stream, AlertCondition.CheckResult checkResult, Configuration config) {
        StringBuilder sb = new StringBuilder();

        if (config.getEmailTransportSubjectPrefix() != null && !config.getEmailTransportSubjectPrefix().isEmpty()) {
            sb.append(config.getEmailTransportSubjectPrefix()).append(" ");
        }

        sb.append("Graylog2 alert for stream: ").append(stream.getTitle());

        return sb.toString();
    }

    private String buildBody(StreamImpl stream, AlertCondition.CheckResult checkResult) {
        StringBuilder sb = new StringBuilder();

        sb.append(checkResult.getResultDescription());

        sb.append("\n\n");
        sb.append("##########\n");
        sb.append("Date: ").append(Tools.iso8601().toString()).append("\n");
        sb.append("Stream ID: ").append(stream.getId()).append("\n");
        sb.append("Stream title: ").append(stream.getTitle()).append("\n");
        if (core.getConfiguration().getEmailTransportWebInterfaceUrl() != null)
            sb.append("Stream URL: ").append(
                    buildStreamDetailsURL(core.getConfiguration().getEmailTransportWebInterfaceUrl(),
                            checkResult, stream));
        sb.append("Stream rules: ").append(stream.getStreamRules()).append("\n");
        sb.append("Alert triggered at: ").append(checkResult.getTriggeredAt()).append("\n");
        sb.append("Triggered condition: ").append(checkResult.getTriggeredCondition()).append("\n");
        sb.append("##########");

        return sb.toString();
    }

    private String buildStreamDetailsURL(URI baseUri, AlertCondition.CheckResult checkResult, StreamImpl stream) {
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


    public void sendEmails(StreamImpl stream, AlertCondition.CheckResult checkResult, List<Message> backlog) throws TransportConfigurationException, EmailException {
        if(!core.getConfiguration().isEmailTransportEnabled()) {
            throw new TransportConfigurationException();
        }

        if (stream.getAlertReceivers() == null || stream.getAlertReceivers().isEmpty()) {
            throw new RuntimeException("Stream [" + stream + "] has no alert receivers.");
        }

        // Send emails to subscribed users.
        if(stream.getAlertReceivers().get("users") != null) {
            for (String username : stream.getAlertReceivers().get("users")) {
                User user = User.load(username, core);

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