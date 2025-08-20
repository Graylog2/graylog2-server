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

import java.net.URI;
import java.util.List;

public record SearchIndexerHosts(List<URI> configuredHosts, List<URI> initialHosts, List<URI> activeHosts) {


    /**
     * This is a workaround for https://github.com/Graylog2/graylog2-server/issues/23057
     * If there are any statically configured hosts, they may also include basic auth credentials. We don't get these
     * credentials from opensearch/elasticsearch client back, only the stripped-down host url. So we'll lose auth information
     * there. For datanode and JWT, this is working fine, as we always use JWT auth header that's not relying on auth
     * credentials in host.
     *
     * The optimal fix would be to somehow extract auth credentials from the opensearch client. IDK if this is anyhow possible
     *
     * When there are any statically configured hosts, ignore the actively obtained and always fallback to configured.
     */
    @Override
    public List<URI> activeHosts() {
        return configuredHosts != null && !configuredHosts.isEmpty() ? configuredHosts : activeHosts;
    }
}
