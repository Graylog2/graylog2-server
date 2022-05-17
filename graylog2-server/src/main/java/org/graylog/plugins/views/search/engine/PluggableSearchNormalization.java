package org.graylog.plugins.views.search.engine;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.permissions.SearchUser;
import org.graylog.plugins.views.search.rest.ExecutionState;

import javax.inject.Inject;
import java.util.Set;

import static com.google.common.base.MoreObjects.firstNonNull;

public class PluggableSearchNormalization implements SearchNormalization {
    private final ObjectMapper objectMapper;
    private final Set<SearchNormalizer> pluggableNormalizers;

    @Inject
    public PluggableSearchNormalization(ObjectMapper objectMapper, Set<SearchNormalizer> pluggableNormalizers) {
        this.objectMapper = objectMapper;
        this.pluggableNormalizers = pluggableNormalizers;
   }

    public Search normalize(Search search, SearchUser searchUser, ExecutionState executionState) {
        final Search searchWithStreams = search.addStreamsToQueriesWithoutStreams(() -> searchUser.streams().loadAll());

        Search normalizedSearch = searchWithStreams.applyExecutionState(objectMapper, firstNonNull(executionState, ExecutionState.empty()));

        for (SearchNormalizer searchNormalizer : pluggableNormalizers) {
            normalizedSearch = searchNormalizer.normalize(normalizedSearch, searchUser, executionState);
        }

        return normalizedSearch;
    }
}
