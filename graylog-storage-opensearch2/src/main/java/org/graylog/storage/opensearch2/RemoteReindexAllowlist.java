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
package org.graylog.storage.opensearch2;

import jakarta.annotation.Nonnull;
import org.apache.commons.lang3.StringUtils;
import org.graylog.shaded.opensearch2.org.apache.lucene.util.automaton.Automaton;
import org.graylog.shaded.opensearch2.org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.graylog.shaded.opensearch2.org.opensearch.common.regex.Regex;

import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class RemoteReindexAllowlist {

    private final URI remoteHost;
    private final String rawValue;
    private final List<String> parsedValue;

    public RemoteReindexAllowlist(URI remoteHost, String requestedAllowlist) {
        this.remoteHost = remoteHost;
        this.rawValue = requestedAllowlist;
        this.parsedValue = parse(remoteHost, requestedAllowlist);
    }

    private static List<String> parse(URI remoteHost, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            // nothing provided, let's use the host address and parse allowlist from it
            return Collections.singletonList(fixProtocolPrefix(remoteHost.toString(), remoteHost));
        } else {
            return Arrays.stream(rawValue.split(","))
                    .map(String::trim)
                    .map(allowlistItem -> fixProtocolPrefix(allowlistItem, remoteHost))
                    .toList();
        }
    }

    public void validate() {
        final String[] patterns = value().toArray(new String[0]);
        Automaton automaton = Regex.simpleMatchToAutomaton(patterns);
        final CharacterRunAutomaton characterRunAutomaton = new CharacterRunAutomaton(automaton);
        final boolean isHostMatching = characterRunAutomaton.run(remoteHost.getHost() + ":" + remoteHost.getPort());
        if (!isHostMatching) {
            throw new IllegalArgumentException("Provided allowlist[" + rawValue + "] doesn't match remote host address[" + remoteHost + "]");
        }
    }

    @Nonnull
    public List<String> value() {
        return parsedValue;
    }


    /**
     * Users often provide the very same value for remote host and allowlist. But allowlist needs to be just
     * hostname:port, without protocol. A mistake that we can easily automatically fix.
     */
    private static String fixProtocolPrefix(String allowlistItem, URI remoteHost) {
        if (remoteHost.toString().equals(allowlistItem)) {
            return allowlistItem.replaceAll("https?://", "");
        }
        return allowlistItem;
    }

    /**
     * Check that actual allowlist in the cluster settings matches the one given for the migration
     */
    public boolean isClusterSettingMatching(String clusterAllowlistSetting) {
        // the value is not proper json, just something like [localhost:9201,my-server:10500].
        final List<String> requested = value();
        final Set<String> existing = parseClusterConfigValue(clusterAllowlistSetting);
        return existing.containsAll(requested) && existing.size() == requested.size();
    }

    @Nonnull
    private static Set<String> parseClusterConfigValue(String clusterAllowlistSetting) {
        return Optional.ofNullable(clusterAllowlistSetting)
                .map(String::trim)
                .map(v -> StringUtils.removeStart(v, "["))
                .map(v -> StringUtils.removeEnd(v, "]"))
                .map(v -> v.split(","))
                .stream()
                .flatMap(Arrays::stream)
                .map(String::trim)
                .collect(Collectors.toSet());
    }
}
