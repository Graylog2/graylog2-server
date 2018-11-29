package org.graylog.plugins.enterprise.search.views;

import com.google.common.base.Functions;
import org.graylog.plugins.enterprise.search.Search;
import org.graylog.plugins.enterprise.search.db.SearchDbService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.search.SearchQuery;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.google.common.base.Strings.isNullOrEmpty;

public class ViewService extends PaginatedDbService<ViewDTO> {
    private static final String COLLECTION_NAME = "views";

    private final ClusterConfigService clusterConfigService;
    private final SearchDbService searchDbService;

    @Inject
    protected ViewService(MongoConnection mongoConnection,
                          MongoJackObjectMapperProvider mapper,
                          ClusterConfigService clusterConfigService,
                          SearchDbService searchDbService) {
        super(mongoConnection, mapper, ViewDTO.class, COLLECTION_NAME);
        this.clusterConfigService = clusterConfigService;
        this.searchDbService = searchDbService;
    }

    public PaginatedList<ViewDTO> searchPaginated(SearchQuery query,
                                                  Predicate<ViewDTO> filter, String order,
                                                  String sortField,
                                                  int page,
                                                  int perPage) {
        return findPaginatedWithQueryFilterAndSort(query.toDBQuery(), filter, getSortBuilder(order, sortField), page, perPage);
    }

    public void saveDefault(ViewDTO dto) {
        if (isNullOrEmpty(dto.id())) {
            throw new IllegalArgumentException("ViewDTO needs an ID to be configured as default view");
        }
        clusterConfigService.write(ViewClusterConfig.builder()
                .defaultViewId(dto.id())
                .build());
    }

    public Optional<ViewDTO> getDefault() {
        return Optional.ofNullable(clusterConfigService.get(ViewClusterConfig.class))
                .flatMap(config -> get(config.defaultViewId()));
    }

    public Collection<ViewParameterSummaryDTO> forValue() {
        final Set<String> searches = this.streamAll()
                .map(ViewDTO::searchId)
                .collect(Collectors.toSet());
        final Map<String, Search> qualifyingSearches = this.searchDbService.findByIds(searches).stream()
                .filter(search -> !search.parameters().isEmpty())
                .collect(Collectors.toMap(Search::id, Functions.identity()));

        return this.streamAll()
                .filter(view -> qualifyingSearches.keySet().contains(view.searchId()))
                .map(view -> ViewParameterSummaryDTO.create(view, qualifyingSearches.get(view.searchId())))
                .collect(Collectors.toSet());
    }
}
