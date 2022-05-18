package org.graylog.plugins.pipelineprocessor.db.mongodb;

import org.graylog.plugins.pipelineprocessor.db.PaginatedPipelineService;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import java.util.function.Predicate;

public class PaginatedMongoDbPipelineService extends PaginatedDbService<PipelineDao> implements PaginatedPipelineService {
    private static final String COLLECTION_NAME = "pipeline_processor_pipelines";

    @Inject
    public PaginatedMongoDbPipelineService(MongoConnection mongoConnection,
                                           MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, PipelineDao.class, COLLECTION_NAME);
    }

    @Override
    public long count() {
        return db.count();
    }

    @Override
    public PaginatedList<PipelineDao> findPaginated(SearchQuery searchQuery, Predicate<PipelineDao> filter, int page, int perPage, String sortField, String order) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryFilterAndSort(dbQuery, filter, sortBuilder, page, perPage);
    }
}
