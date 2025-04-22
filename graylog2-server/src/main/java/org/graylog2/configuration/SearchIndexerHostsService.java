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
package org.graylog2.configuration;

/**
 * This service provides unified access to configured, initial and current list of hosts used in elastic/opensearch
 * clients to access the indexers. The client is the single source of truth when we need to know which all hosts
 * are available. It adds new hosts through sniffers and removes those unavailable.
 */
public interface SearchIndexerHostsService {
    SearchIndexerHosts getHosts();
}
