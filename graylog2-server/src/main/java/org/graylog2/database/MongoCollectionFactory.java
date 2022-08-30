package org.graylog2.database;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.bson.UuidRepresentation;
import org.graylog.shaded.mongojack4.org.mongojack.JacksonMongoCollection;
import org.graylog.shaded.mongojack4.org.mongojack.ObjectMapperConfigurer;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MongoCollectionFactory {
    private final ObjectMapper objectMapper;
    private final MongoConnection mongoConnection;

    @Inject
    public MongoCollectionFactory(ObjectMapper objectMapper, MongoConnection mongoConnection) {
        this.objectMapper = ObjectMapperConfigurer.configureObjectMapper(objectMapper.copy());
        this.mongoConnection = mongoConnection;
    }

    public <T> GraylogMongoCollection<T> create(Class<T> valueType, String collectionName) {
        return new GraylogMongoCollectionImpl<>(createJacksonMongoCollection(valueType, collectionName));
    }

    private <T> JacksonMongoCollection<T> createJacksonMongoCollection(Class<T> valueType, String collectionName) {
        return JacksonMongoCollection.builder()
                .withObjectMapper(objectMapper)
                .build(mongoConnection.getMongoDatabase(), collectionName, valueType, UuidRepresentation.UNSPECIFIED);
    }
}
