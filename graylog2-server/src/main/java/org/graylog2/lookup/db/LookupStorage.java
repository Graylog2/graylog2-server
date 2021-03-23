package org.graylog2.lookup.db;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.WriteConcern;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.lookup.dto.LookupStorageDto;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;
import org.mongojack.WriteResult;

import javax.inject.Inject;

import java.util.Optional;

import static org.graylog2.lookup.dto.LookupStorageDto.FIELD_LOOKUP_KEY;
import static org.graylog2.lookup.dto.LookupStorageDto.FIELD_UPDATED_AT;

/**
 * This class provides MongoDB-backed storage for data retrieved by Data Adapters.  Each Data Adapter's data will
 * be stored in a unique MongoDB collection named "lut_data_adapter_storage_Data-Adapter-ID".  The init() method should
 * be called when the Data Adapter is started and the tearDown() method should be called when the Data Adapter is
 * deleted.
 */
public class LookupStorage {
    private static final String TABLE_PREFIX = "lut_data_adapter_storage_";

    private final JacksonDBCollection<LookupStorageDto, ObjectId> lookupCollection;

    @Inject
    public LookupStorage(MongoConnection mongoConnection,
                         MongoJackObjectMapperProvider objectMapperProvider,
                         String lookupTableId) {
        DB mongoDatabase = mongoConnection.getDatabase();
        DBCollection collection = mongoDatabase.getCollection(TABLE_PREFIX + lookupTableId);

        lookupCollection = JacksonDBCollection.wrap(
                collection,
                LookupStorageDto.class,
                ObjectId.class,
                objectMapperProvider.get());
    }

    /**
     * Set up a new MongoDB collection for a Data Adapter
     */
    public void init() {
        lookupCollection.createIndex(
                new BasicDBObject(FIELD_LOOKUP_KEY, 1),
                new BasicDBObject("unique", true));
    }

    /**
     * Add an entry to a Data Adapter's collection
     * @param key
     * @param data
     */
    public void add(String key, Object data) {
        add(key, data, Tools.nowUTC(), false);
    }

    /**
     * Add an entry to a Data Adapter's collection
     * @param key
     * @param data
     * @param updateTime
     */
    public void add(String key, Object data, DateTime updateTime) {
        add(key, data, updateTime, false);
    }

    /**
     * Add an entry to a Data Adapter's collection.  If the fastWrites parameter is set to TRUE, unacknowledged writes
     * will be used and any errors that occur while writing will not be reported.  This is only intended for use with
     * large data sets (>= 1MM records) which cannot otherwise be processed in a timely fasion.
     * @param key
     * @param data
     * @param updateTime
     * @param fastWrites
     */
    public void add(String key, Object data, DateTime updateTime, boolean fastWrites) {
        final WriteConcern writeConcern = fastWrites ? WriteConcern.UNACKNOWLEDGED : WriteConcern.ACKNOWLEDGED;
        lookupCollection.update(DBQuery.is(FIELD_LOOKUP_KEY, key),
                LookupStorageDto.builder().key(key).data(data).updatedAt(updateTime).build(),
                true,
                false,
                writeConcern);
    }

    /**
     * Retrieve the record for a particular lookup key
     * @param key
     * @return
     */
    public Optional<Object> lookup(String key) {
        LookupStorageDto record = lookupCollection.findOne(DBQuery.is(FIELD_LOOKUP_KEY, key));
        if (null == record) {
            return Optional.empty();
        } else {
            return Optional.ofNullable(record.data());
        }
    }

    /**
     * Delete all records that have not been updated since the cutoff time.
     * @param cutoff
     * @return
     */
    public int deleteNotUpdatedSince(DateTime cutoff) {
        WriteResult result = lookupCollection.remove(DBQuery.lessThan(FIELD_UPDATED_AT, cutoff));
        return result.getN();
    }

    /**
     * Delete all records from the collection.
     * @return
     */
    public int purgeTable() {
        WriteResult result = lookupCollection.remove(DBQuery.exists(FIELD_LOOKUP_KEY));
        return result.getN();
    }

    /**
     * Clean up a collection that is no longer needed.
     */
    public void tearDown() {
        lookupCollection.dropIndexes();
        lookupCollection.drop();
    }
}
