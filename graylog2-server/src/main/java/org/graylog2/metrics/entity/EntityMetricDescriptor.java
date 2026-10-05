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
package org.graylog2.metrics.entity;

import org.graylog2.metrics.entity.cache.EntityCachedMetricDescriptor;

/**
 * Base interface for pluggable entity metric descriptors.
 * <p>
 * The entity type is determined by the Guice named multibinding, not by the descriptor itself.
 * </p>
 * <p>
 * Implementations should use one of the sub-interfaces:
 * <ul>
 *   <li>{@link EntityCachedMetricDescriptor} — for fields cached in MongoDB (e.g. OpenSearch aggregations)</li>
 *   <li>{@link EntityUncachedMetricDescriptor} — for fields computed fresh per request (e.g. domain model queries)</li>
 * </ul>
 * </p>
 */
public interface EntityMetricDescriptor {

    /**
     * The field name this descriptor provides (e.g. "message_count", "pipeline_count").
     */
    String fieldName();
}
