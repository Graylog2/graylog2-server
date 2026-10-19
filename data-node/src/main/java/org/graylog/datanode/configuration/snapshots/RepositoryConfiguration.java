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

import org.apache.commons.lang3.StringUtils;
import org.graylog.datanode.configuration.DatanodeDirectories;
import org.graylog.datanode.configuration.OpensearchConfigurationException;
import org.graylog.datanode.process.configuration.beans.OpensearchKeystoreItem;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * A single snapshot repository definition. There may be more definitions of the same type, each of them has to
 * provide uniquely named opensearch settings (e.g. by using a distinct client name).
 */
public interface RepositoryConfiguration {
    String DEFAULT_CLIENT_NAME = "default";

    boolean isRepositoryEnabled() throws IllegalStateException;

    Map<String, String> opensearchProperties();

    Collection<OpensearchKeystoreItem> keystoreItems(DatanodeDirectories datanodeDirectories);

    /**
     * Resolves a conflict when this repository provides an opensearch property that has been already provided
     * by another repository with a different value. By default, conflicts are not allowed and repositories
     * have to use distinct property names (e.g. distinct client names). Repositories sharing a node-level
     * setting can override this method and merge both values into one.
     *
     * @param key           name of the conflicting property
     * @param existingValue value provided by previously processed repositories
     * @param newValue      value provided by this repository
     * @return merged value of the property
     */
    default String mergeProperty(String key, String existingValue, String newValue) {
        throw new OpensearchConfigurationException(f("Conflicting snapshot repository configuration, property %s is configured with values %s and %s. Please use distinct client names.", key, existingValue, newValue));
    }

    default boolean noneBlank(String... properties) {
        return Arrays.stream(properties).noneMatch(StringUtils::isBlank);
    }

    default boolean allBlank(String... properties) {
        return Arrays.stream(properties).allMatch(StringUtils::isBlank);
    }
}
