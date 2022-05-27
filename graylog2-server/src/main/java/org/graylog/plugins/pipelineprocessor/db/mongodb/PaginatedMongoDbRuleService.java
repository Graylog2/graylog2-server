package org.graylog.plugins.pipelineprocessor.db.mongodb;

import org.graylog.plugins.pipelineprocessor.db.PaginatedRuleService;
import org.graylog.plugins.pipelineprocessor.db.RuleDao;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;

public class PaginatedMongoDbRuleService extends PaginatedDbService<RuleDao> implements PaginatedRuleService {
    private static final String COLLECTION_NAME = "pipeline_processor_rules";

    @Inject
    public PaginatedMongoDbRuleService(MongoConnection mongoConnection,
                                       MongoJackObjectMapperProvider mapper) {
        super(mongoConnection, mapper, RuleDao.class, COLLECTION_NAME);
    }

    public long count() {
        return db.count();
    }

    @Override
    public PaginatedList<RuleDao> findPaginated(SearchQuery searchQuery, int page, int perPage, String sortField, String order) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }
}
