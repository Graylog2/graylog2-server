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
package org.graylog2.system.activities;

import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MongoDBExtension.class)
class SystemMessageServiceImplTest {
    private static final DateTime NOW = new DateTime(2026, 11, 26, 12, 0, DateTimeZone.UTC);

    private SystemMessageServiceImpl service;

    @BeforeEach
    void setUp(MongoCollections mongoCollections) {
        service = new SystemMessageServiceImpl(mongoCollections.mongoConnection());
    }

    private void save(String caller, String content, DateTime timestamp) {
        service.saveWithoutValidation(service.create(Map.of("caller", caller, "content", content,
                "timestamp", timestamp, "node_id", "node-1")));
    }

    private static List<String> contents(PaginatedList<SystemMessage> messages) {
        return messages.stream().map(SystemMessage::getContent).toList();
    }

    @Test
    void findsAllNewestFirstWithoutQuery() {
        save("InputSetupService", "Started input", NOW.minusMinutes(2));
        save("IndexRotationThread", "Rotated index", NOW.minusMinutes(1));

        final PaginatedList<SystemMessage> found = service.find("", null, 1);

        assertThat(contents(found)).containsExactly("Rotated index", "Started input");
        assertThat(found.pagination().total()).isEqualTo(2);
    }

    @Test
    void matchesContentOrCallerIgnoringCase() {
        save("InputSetupService", "Started input <Syslog>", NOW.minusMinutes(3));
        save("IndexRotationThread", "Rotated index graylog_1", NOW.minusMinutes(2));
        save("Other", "Input <Beats> failed", NOW.minusMinutes(1));

        assertThat(contents(service.find("INPUT", null, 1)))
                .containsExactly("Input <Beats> failed", "Started input <Syslog>");
        assertThat(contents(service.find("rotationthread", null, 1))).containsExactly("Rotated index graylog_1");
    }

    @Test
    void treatsTheQueryAsPlainText() {
        save("Caller", "Rotated index graylog_1", NOW.minusMinutes(2));
        save("Caller", "Value (a+b)", NOW.minusMinutes(1));

        assertThat(contents(service.find("(a+b)", null, 1))).containsExactly("Value (a+b)");
        assertThat(contents(service.find(".*", null, 1))).isEmpty();
    }

    @Test
    void limitsBySince() {
        save("Caller", "Old", NOW.minusHours(2));
        save("Caller", "New", NOW.minusMinutes(5));

        final PaginatedList<SystemMessage> found = service.find("", NOW.minusHours(1), 1);

        assertThat(contents(found)).containsExactly("New");
        assertThat(found.pagination().total()).isEqualTo(1);
    }

    @Test
    void pagesThroughMatches() {
        for (int i = 0; i < 35; i++) {
            save("Caller", "Message " + i, NOW.plusSeconds(i));
        }

        final PaginatedList<SystemMessage> second = service.find("message", null, 2);

        assertThat(contents(second)).containsExactly("Message 4", "Message 3", "Message 2", "Message 1", "Message 0");
        assertThat(second.pagination().total()).isEqualTo(35);
        assertThat(second.pagination().page()).isEqualTo(2);
    }

    @Test
    void rejectsPagesBelowOne() {
        assertThatThrownBy(() -> service.find("", null, 0)).isInstanceOf(IllegalArgumentException.class);
    }
}
