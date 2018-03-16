package org.graylog.plugins.enterprise.search.views;

import org.graylog.plugins.enterprise.database.PaginatedDbService;
import org.graylog.plugins.enterprise.database.PaginatedList;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;

public class ViewService extends PaginatedDbService<ViewDTO> {
    private static final String COLLECTION_NAME = "views";

    @Inject
    protected ViewService(MongoConnection mongoConnection, MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, ViewDTO.class, COLLECTION_NAME);
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
}
