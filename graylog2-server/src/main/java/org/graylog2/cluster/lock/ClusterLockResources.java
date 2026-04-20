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
package org.graylog2.cluster.lock;

/**
 * Utility class for generating standardized cluster lock resource names.
 */
public final class ClusterLockResources {
    private ClusterLockResources() {
    }

    public static String indexModification(String indexName) {
        return name("index-modification", indexName);
    }

    public static String indexRangeRecalculation(String indexName) {
        return name("index-range-recalculation", indexName);
    }

    public static String concurrentIndexOptimization() {
        return name("concurrent-index-optimization");
    }

    private static String name(String name, String resource) {
        return "cluster-lock:" + name + ":" + resource;
    }

    private static String name(String name) {
        return "cluster-lock:" + name;
    }
}
