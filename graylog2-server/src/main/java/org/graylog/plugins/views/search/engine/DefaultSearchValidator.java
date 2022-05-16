package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchExecutionGuard;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.errors.IllegalTimeRangeException;
import org.graylog.plugins.views.search.errors.SearchTypeError;
import org.graylog.plugins.views.search.permissions.StreamPermissions;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.joda.time.DateTime;
import org.joda.time.Period;

import javax.inject.Inject;
import java.util.Optional;

public class DefaultSearchValidator implements SearchValidator {
    private final SearchExecutionGuard executionGuard;

    @Inject
    public DefaultSearchValidator(SearchExecutionGuard executionGuard) {
        this.executionGuard = executionGuard;
    }

    public Optional<SearchTypeError> validateSearchType(Query query, SearchType searchType, SearchConfig searchConfig) {
        return searchConfig.getQueryTimeRangeLimit()
                .flatMap(configuredTimeLimit -> searchType.timerange() // TODO: what if there is no timerange for the type but there is a global limit?
                        .map(tr -> tr.effectiveTimeRange(query, searchType))
                        .filter(tr -> isOutOfLimit(tr, configuredTimeLimit))
                        .map(tr -> new SearchTypeError(query, searchType.id(), "Search type '" + searchType.type() + "' out of allowed time range limit")));
    }

    boolean isOutOfLimit(TimeRange timeRange, Period limit) {
        final DateTime start = timeRange.getFrom();
        final DateTime end = timeRange.getTo();
        final DateTime allowedStart = end.minus(limit);
        return start.isBefore(allowedStart);
    }

    public void validateQueryTimeRange(Query query, SearchConfig config) {
        config.getQueryTimeRangeLimit()
                .flatMap(timeRangeLimit -> Optional.ofNullable(query.timerange())
                        .filter(tr -> tr.getFrom() != null && tr.getTo() != null) // TODO: is this check necessary?
                        .filter(tr -> isOutOfLimit(tr, timeRangeLimit)))
                .ifPresent(tr -> {
                    throw new IllegalTimeRangeException("Search out of allowed time range limit");
                });
    }

    public void validate(Search search, StreamPermissions streamPermissions) {
        this.executionGuard.check(search, streamPermissions::canReadStream);
    }
}
