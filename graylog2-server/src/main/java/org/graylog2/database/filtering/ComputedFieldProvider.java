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
package org.graylog2.database.filtering;

import java.util.Set;

/**
 * Provides filtering support for computed/runtime fields that don't exist in the database.
 * <p>
 * Implementations return matching entity IDs based on filter values. The filtering system
 * injects these IDs into the database query using MongoDB's $in operator, allowing efficient
 * filtering on runtime/computed values alongside regular database fields.
 * </p>
 * <p>
 * <b>Example Usage:</b>
 * </p>
 * <pre>
 * // Provider implementation for filtering inputs by runtime status
 * public class InputRuntimeStatusProvider implements ComputedFieldProvider {
 *     &#64;Override
 *     public Set&lt;String&gt; getMatchingIds(String filterValue) {
 *         // Query all cluster nodes for inputs with status = filterValue
 *         // Return set of matching input IDs
 *         return matchingInputIds;
 *     }
 *
 *     &#64;Override
 *     public String getFieldName() {
 *         return "runtime_status";
 *     }
 * }
 * </pre>
 * <p>
 * <b>Integration Flow:</b>
 * </p>
 * <ol>
 *   <li>User applies filter: runtime_status:FAILED</li>
 *   <li>Filter parser detects "runtime_status" is a computed field</li>
 *   <li>Calls InputRuntimeStatusProvider.getMatchingIds("FAILED")</li>
 *   <li>Provider queries all cluster nodes for failed inputs</li>
 *   <li>Returns Set&lt;String&gt; of matching input IDs</li>
 *   <li>Injected into MongoDB query: { _id: { $in: [...ids] } }</li>
 *   <li>MongoDB returns only matching inputs</li>
 * </ol>
 * <p>
 * Providers should be registered with Guice using a Multibinder:
 * </p>
 * <pre>
 * Multibinder&lt;ComputedFieldProvider&gt; binder =
 *     Multibinder.newSetBinder(binder(), ComputedFieldProvider.class);
 * binder.addBinding().to(InputRuntimeStatusProvider.class);
 * </pre>
 */
public interface ComputedFieldProvider {
    /**
     * Returns the set of entity IDs that match the given filter value.
     * <p>
     * This method should:
     * </p>
     * <ul>
     *   <li>Query runtime/computed data sources (registries, caches, external APIs, etc.)</li>
     *   <li>Filter entities based on the provided filter value</li>
     *   <li>Return the IDs of entities that match the criteria</li>
     *   <li>Return an empty set if no entities match or if the filter value is invalid</li>
     * </ul>
     * <p>
     * <b>Performance Considerations:</b>
     * </p>
     * <ul>
     *   <li>This method is called during query construction, so it should be reasonably fast</li>
     *   <li>For cluster-wide data, consider parallel queries to multiple nodes</li>
     *   <li>Consider caching if the computed values change infrequently</li>
     * </ul>
     *
     * @param filterValue The value being filtered on (e.g., "FAILED", "RUNNING")
     * @param authToken Optional authentication token for cluster-wide operations. May be null.
     * @return Set of entity IDs that match the filter value. Empty set if no matches.
     */
    Set<String> getMatchingIds(String filterValue, String authToken);

    /**
     * Returns the field name this provider handles.
     * <p>
     * This name should match the field name used in filter expressions and
     * entity attribute definitions.
     * </p>
     * <p>
     * <b>Example:</b> "runtime_status", "computed_health", "aggregated_state"
     * </p>
     *
     * @return The field name this provider handles (e.g., "runtime_status")
     */
    String getFieldName();
}
