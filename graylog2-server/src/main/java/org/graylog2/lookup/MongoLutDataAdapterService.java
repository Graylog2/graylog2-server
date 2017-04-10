package org.graylog2.lookup;

import com.google.common.collect.ImmutableList;

import com.mongodb.BasicDBObject;

import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.lookup.dto.DataAdapterDto;
import org.graylog2.rest.models.PaginatedList;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import java.util.Iterator;

import javax.inject.Inject;

public class MongoLutDataAdapterService {

    private final JacksonDBCollection<DataAdapterDto, ObjectId> db;

    @Inject
    public MongoLutDataAdapterService(MongoConnection mongoConnection,
                                      MongoJackObjectMapperProvider mapper) {

        db = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("lut_data_adapters"),
                DataAdapterDto.class,
                ObjectId.class,
                mapper.get());

        db.createIndex(new BasicDBObject("name", 1), new BasicDBObject("unique", true));
    }

    public DataAdapterDto get(String idOrName) {
        return db.findOneById(new ObjectId(idOrName));
    }

    public DataAdapterDto save(DataAdapterDto table) {
        WriteResult<DataAdapterDto, ObjectId> save = db.save(table);
        return save.getSavedObject();
    }

    public PaginatedList<DataAdapterDto> findPaginated(DBQuery.Query query, DBSort.SortBuilder sort, int page, int perPage) {

        final DBCursor<DataAdapterDto> cursor = db.find(query)
                .sort(sort)
                .limit(perPage)
                .skip(perPage * Math.max(0, page - 1));

        return new PaginatedList<>(asImmutableList(cursor), cursor.count(), page, perPage);
    }

    private ImmutableList<DataAdapterDto> asImmutableList(Iterator<? extends DataAdapterDto> cursor) {
        return ImmutableList.copyOf(cursor);
    }

    public void delete(String idOrName) {
        db.remove(DBQuery.or(DBQuery.is("_id", idOrName), DBQuery.is("name", idOrName)));
    }
}
