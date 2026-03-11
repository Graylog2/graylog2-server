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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Registry of all computed field providers in the system.
 * <p>
 * This registry maintains a mapping of field names to their corresponding
 * {@link ComputedFieldProvider} implementations. It is used by the filtering
 * system to determine whether a field is computed and to retrieve the appropriate
 * provider for filtering operations.
 * </p>
 * <p>
 * Providers are automatically discovered and registered through Guice's
 * Multibinder mechanism. To add a new computed field provider:
 * </p>
 * <ol>
 *   <li>Create a class that implements {@link ComputedFieldProvider}</li>
 *   <li>Register it with Guice using a Multibinder</li>
 *   <li>The registry will automatically discover and include it</li>
 * </ol>
 * <p>
 * <b>Example Guice Module Configuration:</b>
 * </p>
 * <pre>
 * Multibinder&lt;ComputedFieldProvider&gt; computedFieldBinder =
 *     Multibinder.newSetBinder(binder(), ComputedFieldProvider.class);
 * computedFieldBinder.addBinding().to(InputRuntimeStatusProvider.class);
 * computedFieldBinder.addBinding().to(AnotherComputedFieldProvider.class);
 * </pre>
 * <p>
 * <b>Thread Safety:</b> This class is thread-safe. The internal map is immutable
 * after construction.
 * </p>
 */
@Singleton
public class ComputedFieldRegistry {
    private final Map<String, ComputedFieldProvider> providers;

    /**
     * Constructs the registry by indexing all available providers by their field names.
     * <p>
     * This constructor is called by Guice with all registered {@link ComputedFieldProvider}
     * implementations injected via Multibinder.
     * </p>
     *
     * @param providers Set of all registered computed field providers
     * @throws IllegalStateException if multiple providers claim the same field name
     */
    @Inject
    public ComputedFieldRegistry(Set<ComputedFieldProvider> providers) {
        this.providers = providers.stream()
                .collect(Collectors.toMap(
                        ComputedFieldProvider::getFieldName,
                        Function.identity(),
                        (p1, p2) -> {
                            throw new IllegalStateException(
                                    "Multiple ComputedFieldProviders registered for field: " +
                                            p1.getFieldName() + " (" +
                                            p1.getClass().getName() + " and " +
                                            p2.getClass().getName() + ")"
                            );
                        }
                ));
    }

    /**
     * Retrieves the provider for the specified field name.
     * <p>
     * <b>Example:</b>
     * </p>
     * <pre>
     * Optional&lt;ComputedFieldProvider&gt; provider = registry.getProvider("runtime_status");
     * if (provider.isPresent()) {
     *     Set&lt;String&gt; matchingIds = provider.get().getMatchingIds("RUNNING");
     *     // Use matchingIds in query...
     * }
     * </pre>
     *
     * @param fieldName The name of the computed field
     * @return Optional containing the provider if one exists for the field, empty otherwise
     */
    public Optional<ComputedFieldProvider> getProvider(String fieldName) {
        return Optional.ofNullable(providers.get(fieldName));
    }

    /**
     * Checks whether a field name corresponds to a computed field.
     * <p>
     * This is a convenience method used to quickly determine if a field should
     * be handled by a computed field provider or by the standard database query logic.
     * </p>
     * <p>
     * <b>Example:</b>
     * </p>
     * <pre>
     * if (registry.isComputedField("runtime_status")) {
     *     // Handle as computed field
     * } else {
     *     // Handle as database field
     * }
     * </pre>
     *
     * @param fieldName The name of the field to check
     * @return true if a provider exists for this field, false otherwise
     */
    public boolean isComputedField(String fieldName) {
        return providers.containsKey(fieldName);
    }
}
