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

import org.apache.commons.mail.EmailException;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.alarms.AlertCondition;
import org.graylog2.plugin.alarms.transports.TransportConfigurationException;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.streams.Stream;

import java.util.List;

public interface AlertSender {
    void initialize(Configuration configuration);

    void sendEmails(Stream stream, EmailRecipients recipients, AlertCondition.CheckResult checkResult) throws TransportConfigurationException, EmailException;

    void sendEmails(Stream stream, EmailRecipients recipients, AlertCondition.CheckResult checkResult, List<Message> backlog) throws TransportConfigurationException, EmailException;
}
