/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
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
                .reduce(Predicate::and);
    }
}
