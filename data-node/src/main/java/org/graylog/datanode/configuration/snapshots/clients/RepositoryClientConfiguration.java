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
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreItem;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * A named opensearch repository client with the settings the Graylog server prepared for it, see
 * {@link org.graylog2.datanode.repositories.RepositoryClientSettings}.
 *
 * @param opensearchProperties the regular client settings with their full keys
 * @param keystoreItems        the secure client settings with their full keys, already decrypted
 */
public record RepositoryClientConfiguration(Map<String, String> opensearchProperties,
                                            List<OpensearchKeystoreItem> keystoreItems) implements RepositoryConfiguration {

    @Override
    public boolean isRepositoryEnabled() {
        return true;
    }

    @Override
    public Collection<OpensearchKeystoreItem> keystoreItems(DatanodeDirectories datanodeDirectories) {
        return keystoreItems;
    }
}
