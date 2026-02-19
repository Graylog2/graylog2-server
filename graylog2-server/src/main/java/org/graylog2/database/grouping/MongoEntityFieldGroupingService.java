package org.graylog2.database.grouping;

import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Sorts;
import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.graylog2.database.MongoConnection;
import org.graylog2.shared.security.EntityPermissionsUtils;

import java.util.ArrayList;
import java.util.List;

public class MongoEntityFieldGroupingService implements EntityFieldGroupingService {

    static final String COUNT_FIELD_NAME = "count";
    static final String ID_FIELD_NAME = "_id";
    static final List<String> SORT_BY_ID_FIELDS = List.of(COUNT_FIELD_NAME, ID_FIELD_NAME);
    static final List<String> SORT_BY_VALUE_FIELDS = SORT_BY_ID_FIELDS.reversed();

    private final MongoConnection mongoConnection;
    private final EntityPermissionsUtils permissionsUtils;

    @Inject
    public MongoEntityFieldGroupingService(final MongoConnection mongoConnection,
                                           final EntityPermissionsUtils permissionsUtils) {
        this.mongoConnection = mongoConnection;
        this.permissionsUtils = permissionsUtils;
    }

    @Override
    public List<EntityFieldGroup> groupByField(final String collectionName,
                                               final String fieldName,
                                               final String query,
                                               final String groupFilter,
                                               final int page,
                                               final int pageSize,
                                               final SortOrder sortOrder,
                                               final SortField sortField,
                                               final Subject subject) {
        final MongoCollection<Document> mongoCollection = mongoConnection.getMongoDatabase().getCollection(collectionName);
        final var userCanReadAllEntities = permissionsUtils.hasAllPermission(subject) ||
                permissionsUtils.hasReadPermissionForWholeCollection(subject, collectionName);

        if (userCanReadAllEntities) {
            final AggregateIterable<Document> results = mongoCollection.aggregate(List.of(
                    Aggregates.group("$" + fieldName, Accumulators.sum(COUNT_FIELD_NAME, 1)),
                    buildSortStage(sortOrder, sortField),
                    Aggregates.skip((page - 1) * pageSize),
                    Aggregates.limit(pageSize)
            ));

            List<EntityFieldGroup> result = new ArrayList<>();
            results.forEach(x -> {
                final String id = x.get(ID_FIELD_NAME).toString();
                //it is very likely that BE should fetch related entities and enrich second field of EntityFieldGroup class, instead of using id there
                result.add(new EntityFieldGroup(id, id, x.getInteger(COUNT_FIELD_NAME, 0)));
            });
            return result;
        } else {
            //TODO: undoable in MongoDB, only possible in-memory, but highly inefficient
            return List.of();
        }
    }

    private Bson buildSortStage(final SortOrder sortOrder, final SortField sortField) {
        final List<String> sort = switch (sortField) {
            case COUNT -> SORT_BY_ID_FIELDS;
            case VALUE -> SORT_BY_VALUE_FIELDS;
        };
        return switch (sortOrder) {
            case ASC -> Aggregates.sort(Sorts.ascending(sort));
            case DESC -> Aggregates.sort(Sorts.descending(sort));
        };
    }
}
