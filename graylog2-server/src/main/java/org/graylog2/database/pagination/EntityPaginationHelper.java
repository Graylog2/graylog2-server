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
package org.graylog2.database.pagination;

import com.google.common.annotations.VisibleForTesting;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNDescriptor;
import org.graylog.security.Capability;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public class EntityPaginationHelper {
    private static final Logger LOG = LoggerFactory.getLogger(EntityPaginationHelper.class);
    private static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^(.*):(.*)");

    private EntityPaginationHelper() {
    } // Utility class, no instantiation

    /**
     * Parses a capability filter string and returns an Optional containing the Capability if valid, or empty if invalid.
     *
     * @param capabilityFilterString the capability filter string to parse
     * @return an Optional containing the Capability or empty if the string is invalid
     */
    public static Optional<Capability> parseCapabilityFilter(String capabilityFilterString) {
        if (isNullOrEmpty(capabilityFilterString)) {
            return Optional.empty();
        }
        final String capabilityFilter = capabilityFilterString.trim().toUpperCase(Locale.US);

        try {
            return Optional.of(Capability.valueOf(capabilityFilter));
        } catch (IllegalArgumentException e) {
            LOG.warn("Unknown capability", e);
            return Optional.empty();
        }
    }

    /**
     * Creates a predicate that filters GRNDescriptor objects based on the provided pagination query.
     *
     * @param paginationQuery the pagination query string
     * @return a Predicate that filters GRNDescriptor objects
     */
    public static Predicate<GRNDescriptor> queryPredicate(String paginationQuery) {
        return buildPredicate(paginationQuery, Map.of(
                "type", descriptor -> descriptor.grn().grnType().type(),
                "title", GRNDescriptor::title
        ));
    }

    /**
     * Creates a predicate that filters GRN objects based on the provided list of entity filters.
     *
     * @param filters the list of entity filters
     * @return a Predicate that filters GRN objects
     */
    public static Predicate<GRN> entityFiltersGRNPredicate(List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            return grn -> true;
        }
        return filters.stream()
                .map(EntityPaginationHelper::entityFilterGRNPredicate)
                .reduce(grn -> false, Predicate::or); // Combine all predicates with OR
    }

    /**
     * Creates a predicate that filters GRNDescriptor objects based on the provided list of entity filters.
     *
     * @param filters the list of entity filters
     * @return a Predicate that filters GRNDescriptor objects
     */
    public static Predicate<GRNDescriptor> entityFiltersDescriptorPredicate(List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            return descriptor -> true;
        }
        return filters.stream()
                .map(EntityPaginationHelper::entityFilterDescriptorPredicate)
                .reduce(descriptor -> false, Predicate::or); // Combine all predicates with OR
    }

    @VisibleForTesting
    public static <T> Predicate<T> buildPredicate(String filter, Map<String, Function<T, String>> fieldExtractors) {
        if (isNullOrEmpty(filter) || fieldExtractors.isEmpty()) {
            return t -> true;
        }
        final String trimmedFilter = filter.trim().toLowerCase(Locale.US);

        Matcher m = KEY_VALUE_PATTERN.matcher(trimmedFilter);
        if (m.find()) {
            final String key = m.group(1);
            final String value = m.group(2);
            Function<T, String> extractor = fieldExtractors.get(key);
            if (extractor != null) {
                return o -> {
                    String fieldValue = extractor.apply(o);
                    if (fieldValue != null) {
                        return fieldValue.toLowerCase(Locale.US).contains(value);
                    }
                    return false;
                };
            }
        }

        Set<Predicate<T>> predicates = fieldExtractors.values().stream().map(e ->
                (Predicate<T>) t -> e.apply(t).toLowerCase(Locale.US).contains(trimmedFilter)).collect(Collectors.toSet());

        return t -> predicates.stream().anyMatch(p -> p.test(t));
    }

    private static Predicate<GRN> entityFilterGRNPredicate(String entityFilter) {
        return buildPredicate(entityFilter, Map.of("type", GRN::type));
    }

    private static Predicate<GRNDescriptor> entityFilterDescriptorPredicate(String entityFilter) {
        return buildPredicate(entityFilter, Map.of("type", descriptor -> descriptor.grn().grnType().type()));
    }
}
