package org.graylog.plugins.views.search.engine;

import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchExecutionGuard;
import org.graylog.plugins.views.search.errors.IllegalTimeRangeException;
import org.graylog.plugins.views.search.permissions.StreamPermissions;

import javax.inject.Inject;
import javax.inject.Provider;
import java.util.Optional;

import static org.graylog.plugins.views.search.engine.TimeRangeValidation.isOutOfLimit;

public class DefaultSearchValidator implements SearchValidator {
    private final SearchExecutionGuard executionGuard;
    private final Provider<SearchConfig> searchConfigProvider;

    @Inject
    public DefaultSearchValidator(SearchExecutionGuard executionGuard,
                                  Provider<SearchConfig> searchConfigProvider) {
        this.executionGuard = executionGuard;
        this.searchConfigProvider = searchConfigProvider;
    }

    private void validateQueryTimeRange(Query query, SearchConfig config) {
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

        final SearchConfig searchConfig = searchConfigProvider.get();
        search.queries().forEach(query -> validateQueryTimeRange(query, searchConfig));
    }
}
