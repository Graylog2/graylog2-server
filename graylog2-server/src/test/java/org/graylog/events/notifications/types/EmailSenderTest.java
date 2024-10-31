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

import com.floreysoft.jmte.Engine;
import org.apache.commons.mail.EmailException;
import org.apache.commons.mail.HtmlEmail;
import org.graylog2.bindings.providers.HtmlSafeJmteEngineProvider;
import org.graylog2.notifications.NotificationService;
import org.graylog2.plugin.system.NodeId;
import org.graylog2.shared.email.EmailFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailSenderTest {
    @Mock
    private NotificationService notificationService;
    @Mock
    private NodeId nodeId;
    @Mock
    private EmailFactory emailFactory;
    @Mock
    private HtmlEmail htmlEmail;

    private EmailSender emailSender;

    @BeforeEach
    void setUp() throws EmailException {
        when(emailFactory.htmlEmail()).thenReturn(htmlEmail);

        emailSender = new EmailSender(notificationService, nodeId,
                new Engine(), new HtmlSafeJmteEngineProvider().get(), emailFactory);
    }

    @Test
    void testEmailHtmlEscaping() throws EmailException {
        Map<String, Object> model = Map.of(
                "event_definition_title", "<<Test Event Title>>",
                "event", Map.of("message", "Event Message & Whatnot")
        );
        final EmailEventNotificationConfig config = EmailEventNotificationConfig.builder()
                .htmlBodyTemplate(
                    "Message:              ${event.message}\n" +
                    "Title:                ${event_definition_title}\n"
                )
                .build();

        emailSender.createEmailWithBody(config, model);

        final ArgumentCaptor<String> plainCaptor = ArgumentCaptor.forClass(String.class);
        final ArgumentCaptor<String> htmlCaptor = ArgumentCaptor.forClass(String.class);
        verify(htmlEmail).setTextMsg(plainCaptor.capture());
        verify(htmlEmail).setHtmlMsg(htmlCaptor.capture());

        assertThat(plainCaptor.getValue()).matches(Pattern.compile(".*Title:\\s+<<Test Event Title>>.*", Pattern.DOTALL));
        assertThat(plainCaptor.getValue()).matches(Pattern.compile(".*Message:\\s+Event Message & Whatnot.*", Pattern.DOTALL));

        assertThat(htmlCaptor.getValue()).matches(Pattern.compile(".*Title:\\s+&lt;&lt;Test Event Title&gt;&gt;.*", Pattern.DOTALL));
        assertThat(htmlCaptor.getValue()).matches(Pattern.compile(".*Message:\\s+Event Message &amp; Whatnot.*", Pattern.DOTALL));
    }
}
