/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
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
