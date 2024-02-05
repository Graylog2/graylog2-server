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
package org.graylog.testing.datanode;

import org.graylog2.storage.SearchVersion;

import java.util.Optional;
import java.util.ServiceLoader;

public class DatanodeDevContainerInstanceProvider {
    private static ServiceLoader<DatanodeDevContainerInterfaceProvider> loader = ServiceLoader.load(DatanodeDevContainerInterfaceProvider.class);

    public static Optional<DatanodeDevContainerBuilder> getBuilderFor(SearchVersion searchVersion) {
        for (DatanodeDevContainerInterfaceProvider provider : loader) {
            DatanodeDevContainerBuilder container = provider.getBuilderFor(searchVersion);
            if (container != null) {
                return Optional.of(container);
            }
        }
        return Optional.empty();
    }
}
