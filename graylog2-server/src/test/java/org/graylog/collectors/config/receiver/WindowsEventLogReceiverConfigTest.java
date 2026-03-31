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
package org.graylog.collectors.config.receiver;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WindowsEventLogReceiverConfigTest {
    @Test
    void channelListContainsOnlyCustomChannelsWhenDefaultsDisabled() {
        final var config = receiverConfig(List.of("ForwardedEvents", "Custom/Operational"), false);

        assertThat(config.channelList()).containsExactlyInAnyOrder("ForwardedEvents", "Custom/Operational");
    }

    @Test
    void channelListIncludesDefaultChannelsWhenEnabled() {
        final var config = receiverConfig(List.of("ForwardedEvents"), true);

        assertThat(config.channelList()).containsExactlyInAnyOrder(
                "Application",
                "System",
                "Security",
                "Setup",
                "Microsoft-Windows-Windows Defender/Operational",
                "Microsoft-Windows-TerminalServices-LocalSessionManager/Operational",
                "Microsoft-Windows-PowerShell/Operational",
                "Windows PowerShell",
                "ForwardedEvents"
        );
    }

    @Test
    void channelListDeduplicatesCustomAndDefaultChannels() {
        final var config = receiverConfig(List.of("System", "ForwardedEvents", "ForwardedEvents"), true);

        assertThat(config.channelList()).containsExactlyInAnyOrder(
                "Application",
                "System",
                "Security",
                "Setup",
                "Microsoft-Windows-Windows Defender/Operational",
                "Microsoft-Windows-TerminalServices-LocalSessionManager/Operational",
                "Microsoft-Windows-PowerShell/Operational",
                "Windows PowerShell",
                "ForwardedEvents"
        );
        assertThat(config.channelList()).doesNotHaveDuplicates();
    }

    private static WindowsEventLogReceiverConfig receiverConfig(List<String> channels, boolean includeDefaultChannels) {
        return WindowsEventLogReceiverConfig.builder("windows-eventlog")
                .channels(channels)
                .includeDefaultChannels(includeDefaultChannels)
                .build();
    }
}
