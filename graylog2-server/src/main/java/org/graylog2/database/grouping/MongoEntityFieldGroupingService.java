package org.graylog2.database.grouping;

import com.mongodb.client.MongoCollection;
import jakarta.inject.Inject;
import org.apache.shiro.subject.Subject;
import org.bson.Document;
import org.graylog2.database.MongoConnection;
import org.graylog2.shared.security.EntityPermissionsUtils;

import java.util.List;

public class MongoEntityFieldGroupingService implements EntityFieldGroupingService {

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
                                               final Subject subject) {
        final MongoCollection<Document> mongoCollection = mongoConnection.getMongoDatabase().getCollection(collectionName);
        return List.of();
    }
}
