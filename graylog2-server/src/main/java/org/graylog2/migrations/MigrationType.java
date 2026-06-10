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
package org.graylog2.migrations;

public enum MigrationType {
    /**
     * Will run _before_ preflight checks and before the preflight interface starts
     */
    PREFLIGHT,
    /**
     * Will run during a regular startup, after the preflight is either skipped or finished. This is the default
     * behaviour for most of our migrations.
     */
    STANDARD,
    /**
     * Will run on all nodes in the cluster, regardless of their role.
     *
     * Warning: Executing this migration on follower nodes before the master
     * may lead to inconsistent or invalid state. Only use this type when
     * the migration is guaranteed to be safe in any node execution order.
     */
    ENFORCED_ON_ALL_NODES
}
