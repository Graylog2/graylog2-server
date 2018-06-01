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
