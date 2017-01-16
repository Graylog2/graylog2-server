/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.alarmcallbacks;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.DBCollection;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.CollectionName;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.alarmcallbacks.requests.CreateAlarmCallbackRequest;
import org.mongojack.DBCursor;
import org.mongojack.DBQuery;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlarmCallbackConfigurationServiceImpl implements AlarmCallbackConfigurationService {
    private final JacksonDBCollection<AlarmCallbackConfigurationImpl, String> coll;

    @Inject
    public AlarmCallbackConfigurationServiceImpl(MongoConnection mongoConnection,
                                                 MongoJackObjectMapperProvider mapperProvider) {
        final String collectionName = AlarmCallbackConfigurationImpl.class.getAnnotation(CollectionName.class).value();
        final DBCollection dbCollection = mongoConnection.getDatabase().getCollection(collectionName);
        this.coll = JacksonDBCollection.wrap(dbCollection, AlarmCallbackConfigurationImpl.class, String.class, mapperProvider.get());
        dbCollection.createIndex(AlarmCallbackConfigurationImpl.FIELD_STREAM_ID);
    }

    @Override
    public List<AlarmCallbackConfiguration> getForStreamId(String streamId) {
        return this.toAbstractListType(coll.find(DBQuery.is("stream_id", streamId)).toArray());
    }

    @Override
    public List<AlarmCallbackConfiguration> getForStream(Stream stream) {
        return getForStreamId(stream.getId());
    }

    @Override
    public AlarmCallbackConfiguration load(String alarmCallbackId) {
        return coll.findOneById(alarmCallbackId);
    }

    @Override
    public AlarmCallbackConfiguration create(String streamId, CreateAlarmCallbackRequest request, String userId) {
        return AlarmCallbackConfigurationImpl.create(new ObjectId().toHexString(), streamId, request.type(), request.title(), request.configuration(), new Date(), userId);
    }

    @Override
    public long count() {
        return coll.count();
    }

    @Override
    public Map<String, Long> countPerType() {
        final HashMap<String, Long> result = Maps.newHashMap();

        final DBCursor<AlarmCallbackConfigurationImpl> avs = coll.find();
        for (AlarmCallbackConfigurationImpl av : avs) {
            Long count = result.get(av.getType());
            if (count == null) {
                count = 0L;
            }
            result.put(av.getType(), count + 1);
        }

        return result;
    }

    @Override
    public String save(AlarmCallbackConfiguration model) throws ValidationException {
        return coll.save(implOrFail(model)).getSavedId();
    }

    @Override
    public int destroy(AlarmCallbackConfiguration model) {
        return coll.removeById(model.getId()).getN();
    }

    private List<AlarmCallbackConfiguration> toAbstractListType(List<AlarmCallbackConfigurationImpl> callbacks) {
        final List<AlarmCallbackConfiguration> result = Lists.newArrayList();
        result.addAll(callbacks);

        return result;
    }

    private AlarmCallbackConfigurationImpl implOrFail(AlarmCallbackConfiguration callback) {
        final AlarmCallbackConfigurationImpl callbackImpl;
        if (callback instanceof AlarmCallbackConfigurationImpl) {
            callbackImpl = (AlarmCallbackConfigurationImpl) callback;
            return callbackImpl;
        } else {
            throw new IllegalArgumentException("Supplied output must be of implementation type AlarmCallbackConfigurationAVImpl, not " + callback.getClass());
        }
    }
}
