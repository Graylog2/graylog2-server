package org.graylog.scheduler;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndUpdateOptions;
import com.mongodb.client.model.ReturnDocument;
import com.mongodb.client.model.Updates;
import org.bson.BsonDocument;
import org.bson.BsonDocumentWriter;
import org.bson.codecs.EncoderContext;
import org.graylog.shaded.mongojack4.org.mongojack.JacksonMongoCollection;
import org.graylog2.database.MongoCollections;

import javax.inject.Inject;

import static java.util.Objects.requireNonNull;
import static org.graylog2.shared.utilities.StringUtils.f;

/**
 * The {@link DBJobDefinitionService} is still using the old mongojack version, so we can't implement a
 * {@code findOrCreate} method and have to use this custom service until the class is migrated to the new mongojack
 * version.
 * TODO: Remove once DBJobDefinitionService is migrated to the new mongojack version.
 */
public class DBCustomJobDefinitionService {
    private final JacksonMongoCollection<JobDefinitionDto> db;

    @Inject
    public DBCustomJobDefinitionService(MongoCollections collections) {
        this.db = (JacksonMongoCollection<JobDefinitionDto>) collections.get(DBJobDefinitionService.COLLECTION_NAME, JobDefinitionDto.class);
    }

    public JobDefinitionDto findOrCreate(JobDefinitionDto dto) {
        final var jobDefinitionId = requireNonNull(dto.id(), "Job definition ID cannot be null");

        final var codec = db.getCodecRegistry().get(JobDefinitionDto.class);
        try (final var writer = new BsonDocumentWriter(new BsonDocument())) {
            // Convert the DTO class to a Bson object, so we can use it with $setOnInsert
            codec.encode(writer, dto, EncoderContext.builder().build());

            return db.findOneAndUpdate(
                    Filters.and(
                            Filters.eq("_id", jobDefinitionId),
                            Filters.eq(f("%s.%s", JobDefinitionDto.FIELD_CONFIG, JobDefinitionConfig.TYPE_FIELD), dto.config().type())
                    ),
                    Updates.setOnInsert(writer.getDocument()),
                    new FindOneAndUpdateOptions()
                            .returnDocument(ReturnDocument.AFTER)
                            .upsert(true)
            );
        }
    }
}
