package org.graylog2.lookup;

import com.google.common.collect.ImmutableList;

import com.mongodb.BasicDBObject;

import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.lookup.dto.LookupTableDto;
import org.graylog2.rest.models.PaginatedList;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import java.util.Iterator;

import javax.inject.Inject;

public class MongoLutService {

    private final JacksonDBCollection<LookupTableDto, ObjectId> lutDb;

    @Inject
    public MongoLutService(MongoConnection mongoConnection,
                           MongoJackObjectMapperProvider mapper) {

        lutDb = JacksonDBCollection.wrap(mongoConnection.getDatabase().getCollection("lut_tables"),
                LookupTableDto.class,
                ObjectId.class,
                mapper.get());

        lutDb.createIndex(new BasicDBObject("name", 1), new BasicDBObject("unique", true));
    }

    public LookupTableDto get(String id) {
        return lutDb.findOneById(new ObjectId(id));
    }

    public LookupTableDto save(LookupTableDto table) {
        WriteResult<LookupTableDto, ObjectId> save = lutDb.save(table);
        return save.getSavedObject();
    }

    public PaginatedList<LookupTableDto> findPaginated(DBQuery.Query query, DBSort.SortBuilder sort, int page, int perPage) {

        final DBCursor<LookupTableDto> cursor = lutDb.find(query)
                .sort(sort)
                .limit(perPage)
                .skip(perPage * Math.max(0, page - 1));

        return new PaginatedList<>(asImmutableList(cursor), cursor.count(), page, perPage);
    }

    private ImmutableList<LookupTableDto> asImmutableList(Iterator<? extends LookupTableDto> cursor) {
        return ImmutableList.copyOf(cursor);
    }

    public void delete(String idOrName) {
        lutDb.remove(DBQuery.or(DBQuery.is("_id", idOrName), DBQuery.is("name", idOrName)));
    }
}
