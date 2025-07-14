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

import org.graylog.grn.GRNDescriptor;
import org.graylog.security.Capability;
import org.graylog.security.GrantDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Strings.isNullOrEmpty;

public class EntityPaginationHelper {
    private static final Logger LOG = LoggerFactory.getLogger(EntityPaginationHelper.class);

    public static final Pattern KEY_VALUE_PATTERN = Pattern.compile("^(.*):(.*)");

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

    public static Predicate<GrantDTO> entityFilterGrantPredicate(List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            return descriptor -> true;
        }
        return filters.stream()
                .map(EntityPaginationHelper::entityFilterGrantPredicate)
                .reduce(descriptor -> false, Predicate::or); // Combine all predicates with OR
    }

    public static Predicate<GRNDescriptor> entityFilterPredicate(List<String> filters) {
        if (filters == null || filters.isEmpty()) {
            return descriptor -> true;
        }
        return filters.stream()
                .map(EntityPaginationHelper::entityFilterDescriptorPredicate)
                .reduce(descriptor -> false, Predicate::or); // Combine all predicates with OR
    }

    private static Predicate<GrantDTO> entityFilterGrantPredicate(String entityFilter) {
        if (isNullOrEmpty(entityFilter)) {
            return grantDTO -> true;
        }
        final String filter = entityFilter.trim().toLowerCase(Locale.US);

        Matcher m = KEY_VALUE_PATTERN.matcher(filter);
        if (m.find()) {
            final String key = m.group(1);
            final String value = m.group(2);
            if (key.equals("type")) {
                return grantDTO -> grantDTO.target().grnType().type().toLowerCase(Locale.US).contains(value);
            } else {
                return grantDTO -> false; // Unsupported key, return false
            }
        }

        // If filter is not qualified, we query by type
        return grantDTO -> grantDTO.target().grnType().type().equals(filter);
    }

    private static Predicate<GRNDescriptor> entityFilterDescriptorPredicate(String entityFilter) {
        if (isNullOrEmpty(entityFilter)) {
            return descriptor -> true;
        }
        final String filter = entityFilter.trim().toLowerCase(Locale.US);

        Matcher m = KEY_VALUE_PATTERN.matcher(filter);
        if (m.find()) {
            final String key = m.group(1);
            final String value = m.group(2);
            if (key.equals("type")) {
                return descriptor -> descriptor.grn().grnType().type().toLowerCase(Locale.US).contains(value);
            } else {
                return descriptor -> false; // Unsupported key, return false
            }
        }

        // If filter is not qualified, we query by type
        return descriptor -> descriptor.grn().type().equals(filter);
    }

    public static Predicate<GRNDescriptor> queryPredicate(String paginationQuery) {
        if (isNullOrEmpty(paginationQuery)) {
            return descriptor -> true;
        }
        final String query = paginationQuery.trim().toLowerCase(Locale.US);

        Matcher m = KEY_VALUE_PATTERN.matcher(query);
        if (m.find()) {
            final String key = m.group(1);
            final String value = m.group(2);
            switch (key) {
                case "type":
                    return descriptor -> descriptor.grn().grnType().type().toLowerCase(Locale.US).contains(value);
                case "title":
                    return descriptor -> descriptor.title().toLowerCase(Locale.US).contains(value);
                default:
                    return descriptor -> false;
            }
        }

        // If query is not qualified, we query by title
        return descriptor -> descriptor.title().toLowerCase(Locale.US).contains(query);
    }

}
