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
package org.graylog.datanode.configuration.snapshots.clients;

import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.configuration.snapshots.RepositoryConfiguration;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreFileContentItem;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreItem;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreStringItem;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * A named opensearch repository client with the settings the Graylog server prepared for it, see
 * {@link org.graylog2.datanode.repositories.RepositoryClientSettings}. The secure values are already decrypted.
 */
public record RepositoryClientConfiguration(String type,
                                            String clientName,
                                            Map<String, String> settings,
                                            Map<String, String> secureSettings,
                                            Map<String, String> secureFiles) implements RepositoryConfiguration {

    @Override
    public boolean isRepositoryEnabled() {
        return true;
    }

    @Override
    public Map<String, String> opensearchProperties() {
        return settings.entrySet().stream()
                .collect(Collectors.toMap(entry -> key(entry.getKey()), Map.Entry::getValue));
    }

    @Override
    public Collection<OpensearchKeystoreItem> keystoreItems(DatanodeDirectories datanodeDirectories) {
        return Stream.concat(
                secureSettings.entrySet().stream()
                        .map(entry -> (OpensearchKeystoreItem) new OpensearchKeystoreStringItem(key(entry.getKey()), entry.getValue())),
                secureFiles.entrySet().stream()
                        .map(entry -> new OpensearchKeystoreFileContentItem(key(entry.getKey()), entry.getValue().getBytes(StandardCharsets.UTF_8)))
        ).toList();
    }

    // Contains secrets, the keystore items must not end up in logs
    @Override
    public String toString() {
        return f("RepositoryClientConfiguration[type=%s, clientName=%s, settings=%s]", type, clientName, settings);
    }

    private String key(String setting) {
        return f("%s.client.%s.%s", type, clientName, setting);
    }
}
