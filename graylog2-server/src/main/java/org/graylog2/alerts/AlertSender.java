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
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.streams.StreamImpl;
import org.graylog2.users.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class AlertSender {

    private static final Logger LOG = LoggerFactory.getLogger(AlertSender.class);

    private final Core core;

    public AlertSender(Core core) throws TransportConfigurationException {
        this.core = core;
    }

    public void sendEmails(StreamImpl stream, AlertCondition.CheckResult checkResult) {
        if(!core.getConfiguration().isEmailTransportEnabled()) {
            LOG.info("Email transport is disabled. Not sending email alerts.");
            return;
        }

        if (stream.getAlertReceivers() == null || stream.getAlertReceivers().isEmpty()) {
            LOG.debug("Stream [{}] has no alert receivers.", stream);
            return;
        }

        // Send emails to subscribed users.
        if(stream.getAlertReceivers().get("users") != null) {
            for (String username : stream.getAlertReceivers().get("users")) {
                User user = User.load(username, core);

                if(user != null && user.getEmail() != null && !user.getEmail().isEmpty()) {
                    sendEmail(user.getEmail(), stream, checkResult);
                }
            }
        }

        // Send emails to directly subscribed email addresses.
        if(stream.getAlertReceivers().get("emails") != null) {
            for (String email : stream.getAlertReceivers().get("emails")) {
                if(!email.isEmpty()) {
                    sendEmail(email, stream, checkResult);
                }
            }
        }

    }

    private void sendEmail(String emailAddress, StreamImpl stream, AlertCondition.CheckResult checkResult) {
        try {
            Email email = new SimpleEmail();
            email.setHostName(core.getConfiguration().getEmailTransportHostname());
            email.setSmtpPort(core.getConfiguration().getEmailTransportPort());

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
            email.setMsg(buildBody(stream, checkResult));
            email.addTo(emailAddress);

            email.send();
        } catch(EmailException e) {
            LOG.error("Could not send alert email.", e);
        }
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
        sb.append("Stream rules: ").append(stream.getStreamRules()).append("\n");
        sb.append("Alert triggered at: ").append(checkResult.getTriggeredAt()).append("\n");
        sb.append("Triggered condition: ").append(checkResult.getTriggeredCondition()).append("\n");
        sb.append("##########");

        return sb.toString();
    }


}