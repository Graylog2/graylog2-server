package org.graylog.plugins.views.search.engine.validation;

import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchExecutionGuard;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.permissions.StreamPermissions;

import javax.inject.Inject;
import java.util.Set;
import java.util.stream.Collectors;

public class PluggableSearchValidation implements SearchValidation {
    private final SearchExecutionGuard executionGuard;
    private final Set<SearchValidator> pluggableSearchValidators;

    @Inject
    public PluggableSearchValidation(SearchExecutionGuard executionGuard,
                                     Set<SearchValidator> pluggableSearchValidators) {
        this.executionGuard = executionGuard;
        this.pluggableSearchValidators = pluggableSearchValidators;
    }

    public Set<SearchError> validate(Search search, StreamPermissions streamPermissions) {
        this.executionGuard.check(search, streamPermissions::canReadStream);

        return this.pluggableSearchValidators.stream()
                .flatMap(validator -> validator.validate(search, streamPermissions).stream())
                .collect(Collectors.toSet());
    }
}
