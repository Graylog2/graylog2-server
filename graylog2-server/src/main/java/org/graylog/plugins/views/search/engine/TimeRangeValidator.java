package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.errors.QueryError;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.errors.SearchTypeError;
import org.graylog.plugins.views.search.permissions.StreamPermissions;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.graylog.plugins.views.search.engine.TimeRangeValidation.isOutOfLimit;

public class TimeRangeValidator implements SearchValidator{
    private final Provider<SearchConfig> searchConfigProvider;

    @Inject
    public TimeRangeValidator(Provider<SearchConfig> searchConfigProvider) {
        this.searchConfigProvider = searchConfigProvider;
    }

    private Stream<SearchError> validateQueryTimeRange(Query query, SearchConfig config) {
        final Optional<SearchError> queryError = config.getQueryTimeRangeLimit()
                .flatMap(timeRangeLimit -> Optional.ofNullable(query.timerange())
                        .filter(tr -> tr.getFrom() != null && tr.getTo() != null) // TODO: is this check necessary?
                        .filter(tr -> isOutOfLimit(tr, timeRangeLimit)))
                .map(tr -> new QueryError(query, "Search out of allowed time range limit"));

        final Stream<SearchError> searchTypeErrors = query.searchTypes()
                .stream()
                .flatMap(searchType -> validateSearchType(query, searchType, config).map(Stream::of).orElseGet(Stream::empty));
        return Stream.concat(queryError.map(Stream::of).orElseGet(Stream::empty), searchTypeErrors);
    }

    private Optional<SearchTypeError> validateSearchType(Query query, SearchType searchType, SearchConfig searchConfig) {
        return searchConfig.getQueryTimeRangeLimit()
                .flatMap(configuredTimeLimit -> searchType.timerange() // TODO: what if there is no timerange for the type but there is a global limit?
                        .map(tr -> tr.effectiveTimeRange(query, searchType))
                        .filter(tr -> isOutOfLimit(tr, configuredTimeLimit))
                        .map(tr -> new SearchTypeError(query, searchType.id(), "Search type '" + searchType.type() + "' out of allowed time range limit")));
    }


    public Set<SearchError> validate(Search search, StreamPermissions streamPermissions) {
        final SearchConfig searchConfig = searchConfigProvider.get();
        return search.queries()
                .stream()
                .flatMap(query -> validateQueryTimeRange(query, searchConfig))
                .collect(Collectors.toSet());
    }
}
