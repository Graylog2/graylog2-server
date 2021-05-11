package org.graylog.plugins.views.search.export;

import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.joda.time.DateTime;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;
import java.util.Optional;

public class ExportJobService {
    protected final JacksonDBCollection<ExportJob, ObjectId> db;

    @Inject
    public ExportJobService(MongoConnection mongoConnection,
                            MongoJackObjectMapperProvider mapper) {
        db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("export_jobs"),
                ExportJob.class,
                ObjectId.class,
                mapper.get());

    }

    public Optional<ExportJob> get(String id) {
        return Optional.ofNullable(db.findOneById(new ObjectId(id)));
    }

    public String save(ExportJob exportJob) {
        final WriteResult<ExportJob, ObjectId> save = db.insert(exportJob);

        return save.getSavedId().toHexString();
    }

    public void removeExpired(DateTime olderThan) {
        db.remove(DBQuery.lessThan(ExportJob.FIELD_CREATED_AT, olderThan));
    }
}
