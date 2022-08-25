package org.graylog.plugins.views.search.searchfilters;

import org.graylog.plugins.views.search.searchfilters.model.ReferencedSearchFilter;
import org.graylog.plugins.views.search.searchfilters.model.UsesSearchFilters;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class ReferencedSearchFiltersRetriever {

    public Set<String> getReferencedSearchFiltersIds(final Collection<UsesSearchFilters> searchFiltersOwners) {
        return searchFiltersOwners
                .stream()
                .map(UsesSearchFilters::filters)
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .filter(usedSearchFilter -> usedSearchFilter instanceof ReferencedSearchFilter)
                .map(usedSearchFilter -> (ReferencedSearchFilter) usedSearchFilter)
                .map(ReferencedSearchFilter::id)
                .collect(Collectors.toSet());
    }
}
