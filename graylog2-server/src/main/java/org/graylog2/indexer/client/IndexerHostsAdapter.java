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
package org.graylog2.indexer.client;

import java.net.URI;
import java.util.List;

public interface IndexerHostsAdapter {
    /**
     * @return List of up-to-date nodes as the elastic/opensearch client sees them and uses them. This includes all
     * changes caused by sniffers and dynamic infrastructure changes. Unlike the fixed "elasticsearch_hosts" configuration
     * property, this may change during every call and gives real-time information.
     */
    List<URI> getActiveHosts();
}
