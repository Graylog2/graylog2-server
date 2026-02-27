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
package org.graylog.collectors.config;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class WindowsEventLogReceiverConfigTest {
    private static final Pattern QUERY_ELEMENT_PATTERN = Pattern.compile(
            "<Query Id=\"(\\d+)\" Path=\"([^\"]+)\">\\s*<Select Path=\"([^\"]+)\">\\*</Select>\\s*</Query>");

    @Test
    void queryContainsOnlyCustomChannelsWhenDefaultsDisabled() {
        final var config = receiverConfig(List.of("ForwardedEvents", "Custom/Operational"), false);

        final var elements = queryElements(config.query());
        final var paths = elements.stream().map(QueryElement::queryPath).toList();
        final var ids = elements.stream().map(QueryElement::id).toList();

        assertThat(paths).containsExactlyInAnyOrder("ForwardedEvents", "Custom/Operational");
        assertThat(ids).containsExactlyInAnyOrder(0, 1);
        assertThat(elements).allSatisfy(element -> assertThat(element.queryPath()).isEqualTo(element.selectPath()));
    }

    @Test
    void queryIncludesDefaultChannelsWhenEnabled() {
        final var config = receiverConfig(List.of("ForwardedEvents"), true);

        final var elements = queryElements(config.query());
        final var paths = elements.stream().map(QueryElement::queryPath).toList();

        assertThat(paths).containsExactlyInAnyOrderElementsOf(Set.of(
                "Application",
                "System",
                "Security",
                "Setup",
                "Microsoft-Windows-Windows Defender/Operational",
                "Microsoft-Windows-TerminalServices-LocalSessionManager/Operational",
                "Microsoft-Windows-PowerShell/Operational",
                "Windows PowerShell",
                "ForwardedEvents"
        ));
        assertThat(elements).allSatisfy(element -> assertThat(element.queryPath()).isEqualTo(element.selectPath()));
    }

    @Test
    void queryDeduplicatesCustomAndDefaultChannels() {
        final var config = receiverConfig(List.of("System", "ForwardedEvents", "ForwardedEvents"), true);

        final var elements = queryElements(config.query());
        final var paths = elements.stream().map(QueryElement::queryPath).toList();

        assertThat(paths).containsExactlyInAnyOrderElementsOf(Set.of(
                "Application",
                "System",
                "Security",
                "Setup",
                "Microsoft-Windows-Windows Defender/Operational",
                "Microsoft-Windows-TerminalServices-LocalSessionManager/Operational",
                "Microsoft-Windows-PowerShell/Operational",
                "Windows PowerShell",
                "ForwardedEvents"
        ));
        assertThat(paths).doesNotHaveDuplicates();
    }

    private static WindowsEventLogReceiverConfig receiverConfig(List<String> channels, boolean includeDefaultChannels) {
        return WindowsEventLogReceiverConfig.builder("windows-eventlog")
                .channels(channels)
                .includeDefaultChannels(includeDefaultChannels)
                .build();
    }

    private static List<QueryElement> queryElements(String query) {
        final var matcher = QUERY_ELEMENT_PATTERN.matcher(query);
        final var elements = new java.util.ArrayList<QueryElement>();
        while (matcher.find()) {
            elements.add(new QueryElement(
                    Integer.parseInt(matcher.group(1)),
                    matcher.group(2),
                    matcher.group(3)
            ));
        }
        return elements;
    }

    private record QueryElement(int id, String queryPath, String selectPath) {}
}
