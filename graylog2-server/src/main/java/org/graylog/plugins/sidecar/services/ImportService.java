package org.graylog.plugins.sidecar.services;

import org.bson.types.ObjectId;
import org.graylog.plugins.sidecar.rest.models.CollectorUpload;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ImportService extends PaginatedDbService<CollectorUpload> {
    private static final String COLLECTION_NAME = "collector_uploads";
    private final JacksonDBCollection<CollectorUpload, ObjectId> dbCollection;

    @Inject
    public ImportService(MongoConnection mongoConnection,
                         MongoJackObjectMapperProvider mapper){
        super(mongoConnection, mapper, CollectorUpload.class, COLLECTION_NAME);
        dbCollection = JacksonDBCollection.wrap(
                mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                CollectorUpload.class,
                ObjectId.class,
                mapper.get());
    }

    public PaginatedList<CollectorUpload> findPaginated(SearchQuery searchQuery, int page, int perPage, String sortField, String order) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }


    public List<CollectorUpload> all() {
        try (final Stream<CollectorUpload> collectorUploadStream = streamAll()) {
            return collectorUploadStream.collect(Collectors.toList());
        }
    }

    public long count() {
        return db.count();
    }
}
