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
package org.graylog.plugins.sidecar.filter;


import org.graylog.plugins.sidecar.rest.models.Sidecar;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public class AdministrationFiltersFactory {
    private final AdministrationFilter.Factory administrationFilterFactory;

    @Inject
    public AdministrationFiltersFactory(AdministrationFilter.Factory administrationFilterFactory) {
        this.administrationFilterFactory = administrationFilterFactory;
    }

    public Optional<Predicate<Sidecar>> getFilters(Map<String, String> filters) {
        return filters.entrySet().stream()
                .map((Function<Map.Entry<String, String>, Predicate<Sidecar>>) entry -> {
                    final String name = entry.getKey();
                    final String value = entry.getValue();

                    final AdministrationFilter.Type filter = AdministrationFilter.Type.valueOf(name.toUpperCase(Locale.ENGLISH));
                    switch (filter) {
                        case COLLECTOR:
                            return administrationFilterFactory.createCollectorFilter(value);
                        case CONFIGURATION:
                            return administrationFilterFactory.createConfigurationFilter(value);
                        case OS:
                            return administrationFilterFactory.createOsFilter(value);
                        case STATUS:
                            return administrationFilterFactory.createStatusFilter(Integer.valueOf(value));
                    }
                    return null;
                })
                .filter(Objects::nonNull)
                // We join all filters with an and condition, so results will need to match all filters
                .reduce(Predicate::and);
    }
}
