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
package org.graylog.collectors.events;

/**
 * Posted on the local event bus after this node's {@link org.graylog.collectors.CollectorsConfigService} has
 * invalidated its cache in response to a collectors config change.
 * <p>
 * Subscribe to this event — not to {@link org.graylog2.cluster.ClusterConfigChangedEvent} — to react to collectors
 * config changes: subscribers of the raw cluster event race the cache invalidation on the async event bus and can
 * read a stale config, while this event is guaranteed to be delivered after the cache is fresh.
 */
public record CollectorsConfigUpdatedEvent() {
}
