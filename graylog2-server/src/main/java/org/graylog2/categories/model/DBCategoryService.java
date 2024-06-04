package org.graylog2.categories.model;

import com.google.common.collect.ImmutableMap;
import com.mongodb.BasicDBObject;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.entities.EntityScopeService;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.database.utils.ScopedEntityMongoUtils;
import org.graylog2.search.SearchQuery;
import org.graylog2.search.SearchQueryField;
import org.graylog2.search.SearchQueryParser;

import jakarta.inject.Inject;

import java.util.Optional;
import java.util.function.Predicate;

import static com.mongodb.client.model.Filters.eq;

public class DBCategoryService {
    public static final String COLLECTION_NAME = "categories";

    private static final ImmutableMap<String, SearchQueryField> ALLOWED_FIELDS = ImmutableMap.<String, SearchQueryField>builder()
            .put(Category.FIELD_CATEGORY, SearchQueryField.create(Category.FIELD_CATEGORY))
            .build();

    private final MongoCollection<Category> collection;
    private final MongoUtils<Category> mongoUtils;
    private final ScopedEntityMongoUtils<Category> scopedEntityMongoUtils;
    private final MongoPaginationHelper<Category> paginationHelper;
    private final SearchQueryParser searchQueryParser;

    @Inject
    public DBCategoryService(MongoCollections mongoCollections,
                             EntityScopeService entityScopeService) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, Category.class);
        this.mongoUtils = mongoCollections.utils(collection);
        this.scopedEntityMongoUtils = mongoCollections.scopedEntityUtils(collection, entityScopeService);
        this.paginationHelper = mongoCollections.paginationHelper(collection);
        this.searchQueryParser = new SearchQueryParser(Category.FIELD_CATEGORY, ALLOWED_FIELDS);

        IndexOptions indexOptions = new IndexOptions().unique(true);
        collection.createIndex(new BasicDBObject(Category.FIELD_CATEGORY, 1), indexOptions);
    }

    public Optional<Category> get(String id) {
        return mongoUtils.getById(id);
    }

    public PaginatedList<Category> findPaginated(String query, int page, int perPage, Bson sort, Predicate<Category> filter) {
        final SearchQuery searchQuery = searchQueryParser.parse(query);
        return filter == null ?
                paginationHelper.filter(searchQuery.toBson()).sort(sort).perPage(perPage).page(page) :
                paginationHelper.filter(searchQuery.toBson()).sort(sort).perPage(perPage).page(page, filter);
    }

    public Category save(Category category) {
        if (category.id() != null) {
            return scopedEntityMongoUtils.update(category);
        }
        String newId = scopedEntityMongoUtils.create(category);
        return category.toBuilder().id(newId).build();
    }

    public Optional<Category> getByValue(String value) {
        final Bson query = eq(Category.FIELD_CATEGORY, value);

        return Optional.ofNullable(collection.find(query).first());
    }

    public long delete(String id) {
        return collection.deleteOne(MongoUtils.idEq(id)).getDeletedCount();
    }
}
