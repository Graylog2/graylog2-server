package org.graylog2.inputs;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollection;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.rest.models.SortOrder;

import java.util.function.Predicate;

@Singleton
public class PaginatedInputService {

    private static final String COLLECTION_NAME = "inputs";
    private final MongoCollection<ShinyInputImpl> collection;
    private final MongoPaginationHelper<ShinyInputImpl> paginationHelper;

    @Inject
    public  PaginatedInputService(MongoCollections mongoCollections) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, ShinyInputImpl.class);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
    }

    public PaginatedList<ShinyInputImpl> findPaginated(Bson searchQuery,
                                                  Predicate<ShinyInputImpl> filter,
                                                  SortOrder order,
                                                  String sortField,
                                                  int page,
                                                  int perPage) {
        return paginationHelper.perPage(perPage)
                .sort(order.toBsonSort(sortField))
                .filter(searchQuery)
                .page(page, filter);
    }
}
