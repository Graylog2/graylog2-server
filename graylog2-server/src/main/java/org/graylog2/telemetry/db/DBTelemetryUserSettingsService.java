package org.graylog2.telemetry.db;

import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import java.util.Optional;

import static org.graylog2.telemetry.db.TelemetryUserSettingsDto.FIELD_USER_ID;

public class DBTelemetryUserSettingsService {

    public static final String COLLECTION_NAME = "telemetry_user_settings";

    private final JacksonDBCollection<TelemetryUserSettingsDto, ObjectId> db;

    @Inject
    public DBTelemetryUserSettingsService(MongoConnection mongoConnection,
                                          MongoJackObjectMapperProvider mapper) {
        this.db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection(COLLECTION_NAME),
                TelemetryUserSettingsDto.class,
                ObjectId.class,
                mapper.get());
    }

    public Optional<TelemetryUserSettingsDto> findByUserId(String userId) {
        return Optional.ofNullable(db.findOne(DBQuery.is(FIELD_USER_ID, userId)));
    }

    public void save(TelemetryUserSettingsDto dto) {
        db.save(dto);
    }
}
