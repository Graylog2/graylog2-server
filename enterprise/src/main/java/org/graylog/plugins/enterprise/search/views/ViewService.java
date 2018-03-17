package org.graylog.plugins.enterprise.search.views;

import org.graylog.plugins.enterprise.database.PaginatedDbService;
import org.graylog.plugins.enterprise.database.PaginatedList;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.Optional;

import static com.google.common.base.Strings.isNullOrEmpty;

public class ViewService extends PaginatedDbService<ViewDTO> {
    private static final String COLLECTION_NAME = "views";

    private final ClusterConfigService clusterConfigService;

    @Inject
    protected ViewService(MongoConnection mongoConnection,
                          MongoJackObjectMapperProvider mapper,
                          ClusterConfigService clusterConfigService) {
        super(mongoConnection, mapper, ViewDTO.class, COLLECTION_NAME);
        this.clusterConfigService = clusterConfigService;
    }

    public PaginatedList<ViewDTO> searchPaginated(SearchQuery query, String order, String sort, int page, int perPage) {
        DBSort.SortBuilder sortBuilder;
        if ("desc".equalsIgnoreCase(order)) {
            sortBuilder = DBSort.desc(sort);
        } else {
            sortBuilder = DBSort.asc(sort);
        }

        return findPaginatedWithQueryAndSort(query.toDBQuery(), sortBuilder, page, perPage);
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
}
