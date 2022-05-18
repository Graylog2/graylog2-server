package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.errors.SearchTypeError;

import java.util.Optional;

import static org.graylog.plugins.views.search.engine.TimeRangeValidation.isOutOfLimit;

public class SearchTypeValidator {
    public static Optional<SearchTypeError> validate(Query query, SearchType searchType, SearchConfig searchConfig) {
        return searchConfig.getQueryTimeRangeLimit()
                .flatMap(configuredTimeLimit -> searchType.timerange() // TODO: what if there is no timerange for the type but there is a global limit?
                        .map(tr -> tr.effectiveTimeRange(query, searchType))
                        .filter(tr -> isOutOfLimit(tr, configuredTimeLimit))
                        .map(tr -> new SearchTypeError(query, searchType.id(), "Search type '" + searchType.type() + "' out of allowed time range limit")));
    }
}
