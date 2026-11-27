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
package org.graylog.events.notifications.types;

import org.apache.commons.mail.EmailException;
import org.junit.jupiter.api.Test;

import javax.mail.MessagingException;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;

class EmailEventNotificationTest {
    @Test
    void describesExceptionWithItsCauses() {
        final EmailException e = new EmailException("Sending the email to the following server failed : smtp.example.com:587",
                new MessagingException("Failed to obtain OAuth 2.0 access token: HTTP 401 (invalid_client: AADSTS7000215)"));

        assertThat(EmailEventNotification.describeWithCauses(e)).isEqualTo(
                "Sending the email to the following server failed : smtp.example.com:587: " +
                        "Failed to obtain OAuth 2.0 access token: HTTP 401 (invalid_client: AADSTS7000215)");
    }

    @Test
    void skipsMessagesRepeatedByWrappingExceptions() {
        final RuntimeException e = new RuntimeException(new IOException("Connection refused"));

        assertThat(EmailEventNotification.describeWithCauses(e)).isEqualTo("java.io.IOException: Connection refused");
    }

    @Test
    void fallsBackToExceptionTypeWithoutMessages() {
        assertThat(EmailEventNotification.describeWithCauses(new NullPointerException())).isEqualTo("java.lang.NullPointerException");
    }
}
