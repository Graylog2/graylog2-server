package org.graylog.plugins.views.search.engine.validation;

import com.google.common.collect.ImmutableSet;
import org.graylog.plugins.views.search.DataWarehouseSearchType;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.SearchType;
import org.graylog.plugins.views.search.errors.QueryError;
import org.graylog.plugins.views.search.errors.SearchError;
import org.graylog.plugins.views.search.errors.SearchTypeError;
import org.graylog.plugins.views.search.permissions.SearchUser;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class DataWarehouseSearchValidator implements SearchValidator {

    @Override
    public Set<SearchError> validate(final Search search,
                                     final SearchUser searchUser) {
        //this should be either validated elsewhere or impossible
        assert search.queries() != null;
        assert !search.queries().isEmpty();

        if (containsDataWarehouseSearchElements(search)) {
            if (search.queries().size() > 1) {
                return wholeSearchInvalid(search, "Data Warehouse elements present in Search, only 1 query allowed for those type of searches");
            }
            return validate(search.queries().stream().findFirst().get(), searchUser);
        } else {
            return Set.of();
        }
    }

    @Override
    public Set<SearchError> validate(final Query query,
                                     final SearchUser searchUser) {
        if (containsDataWarehouseSearchElements(query)) {
            final ImmutableSet<SearchType> searchTypes = query.searchTypes();
            if (searchTypes.size() != 1) {
                return Set.of(new QueryError(query, "Data Warehouse query can contain only one search type"));
            }
            final Optional<SearchType> first = searchTypes.stream().findFirst();
            if (!(first.get() instanceof DataWarehouseSearchType)) {
                return Set.of(new SearchTypeError(query, first.get().id(), "Data Warehouse query can contain only data warehouse search types"));
            } else {
                final Set<String> streams = first.get().streams();
                if (streams == null || streams.size() > 1) {
                    return Set.of(new SearchTypeError(query, first.get().id(), "Data Warehouse preview can be executed on only 1 stream, search type contained more"));
                }
            }
        }
        return Set.of();
    }

    private boolean containsDataWarehouseSearchElements(final Search search) {
        return search.queries().stream().anyMatch(this::containsDataWarehouseSearchElements);
    }

    private boolean containsDataWarehouseSearchElements(final Query query) {
        return (query.query().type().startsWith(DataWarehouseSearchType.PREFIX))
                || (query.searchTypes().stream().anyMatch(searchType -> searchType instanceof DataWarehouseSearchType));
    }

    private Set<SearchError> wholeSearchInvalid(final Search search, final String explanation) {
        return search.queries()
                .stream()
                .map(query -> new QueryError(query, explanation))
                .collect(Collectors.toSet());
    }
}
