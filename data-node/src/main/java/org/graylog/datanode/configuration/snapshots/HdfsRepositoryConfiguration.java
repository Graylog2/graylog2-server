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
package org.graylog.datanode.configuration.snapshots;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.documentation.Documentation;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreItem;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class HdfsRepositoryConfiguration implements RepositoryConfiguration {

    @Documentation("Should HDFS repository be enabled? This will also add the search role and activate search cache.")
    @Parameter(value = "hdfs_repository_enabled")
    private boolean enabled = false;

    @Override
    public boolean isRepositoryEnabled() {
        return enabled;
    }

    @Override
    public Map<String, String> opensearchProperties() {
        return Collections.emptyMap();
    }

    @Override
    public Collection<OpensearchKeystoreItem> keystoreItems(DatanodeDirectories datanodeDirectories) {
        return Collections.emptyList();
    }
}
