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

import java.util.Collection;

/**
 * Source of snapshot repository definitions. Implementations are collected via a Guice multibinder, so plugins
 * can contribute additional repositories next to the ones defined in the datanode configuration file.
 * The provider is queried every time the opensearch configuration is (re)built.
 */
public interface RepositoryConfigurationProvider {
    Collection<RepositoryConfiguration> get();
}
