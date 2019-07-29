package org.graylog.events.processor;

import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public class DBEventDefinitionService extends PaginatedDbService<EventDefinitionDto> {
    private static final Logger LOG = LoggerFactory.getLogger(DBEventDefinitionService.class);

    private static final String COLLECTION_NAME = "event_definitions";

    private final DBEventProcessorStateService stateService;

    @Inject
    public DBEventDefinitionService(MongoConnection mongoConnection,
                                    MongoJackObjectMapperProvider mapper,
                                    DBEventProcessorStateService stateService) {
        super(mongoConnection, mapper, EventDefinitionDto.class, COLLECTION_NAME);
        this.stateService = stateService;
    }

    public PaginatedList<EventDefinitionDto> getAllPaginated(String sortByField, int page, int perPage) {
        return findPaginatedWithQueryAndSort(DBQuery.empty(), DBSort.asc(sortByField), page, perPage);
    }

    public PaginatedList<EventDefinitionDto> getAllPaginated(SearchQuery query, String sortByField, int page, int perPage) {
        return findPaginatedWithQueryAndSort(query.toDBQuery(), DBSort.asc(sortByField), page, perPage);
    }

    @Override
    public int delete(String id) {
        try {
            stateService.deleteByEventDefinitionId(id);
        } catch (Exception e) {
            LOG.error("Couldn't delete event processor state for <{}>", id, e);
        }
        return super.delete(id);
    }
}
