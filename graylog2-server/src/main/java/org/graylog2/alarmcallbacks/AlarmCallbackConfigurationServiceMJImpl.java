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
import com.mongodb.DBCollection;
import org.bson.types.ObjectId;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.CollectionName;
import org.graylog2.database.MongoConnection;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.rest.models.alarmcallbacks.requests.CreateAlarmCallbackRequest;
import org.mongojack.DBQuery;
import org.mongojack.DBUpdate;
import org.mongojack.JacksonDBCollection;

import javax.inject.Inject;
import java.util.Date;
import java.util.List;
import java.util.Map;

public class AlarmCallbackConfigurationServiceMJImpl implements AlarmCallbackConfigurationService {
    private final JacksonDBCollection<AlarmCallbackConfigurationAVImpl, String> coll;

    @Inject
    public AlarmCallbackConfigurationServiceMJImpl(MongoConnection mongoConnection,
                                                   MongoJackObjectMapperProvider mapperProvider) {
        final String collectionName = AlarmCallbackConfigurationAVImpl.class.getAnnotation(CollectionName.class).value();
        final DBCollection dbCollection = mongoConnection.getDatabase().getCollection(collectionName);
        this.coll = JacksonDBCollection.wrap(dbCollection, AlarmCallbackConfigurationAVImpl.class, String.class, mapperProvider.get());
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
        return AlarmCallbackConfigurationAVImpl.create(new ObjectId().toHexString(), streamId, request.type, request.configuration, new Date(), userId);
    }

    @Override
    public long count() {
        return coll.count();
    }

    @Override
    public AlarmCallbackConfiguration update(String streamId, String alarmCallbackId, Map<String, Object> deltas) {
        DBUpdate.Builder update = new DBUpdate.Builder();
        for (Map.Entry<String, Object> fields : deltas.entrySet())
            update = update.set(fields.getKey(), fields.getValue());

        return coll.findAndModify(DBQuery.is("_id", alarmCallbackId), update);
    }

    @Override
    public String save(AlarmCallbackConfiguration model) throws ValidationException {
        return coll.save(implOrFail(model)).getSavedId();
    }

    @Override
    public int destroy(AlarmCallbackConfiguration model) {
        return coll.removeById(model.getId()).getN();
    }

    private List<AlarmCallbackConfiguration> toAbstractListType(List<AlarmCallbackConfigurationAVImpl> callbacks) {
        final List<AlarmCallbackConfiguration> result = Lists.newArrayList();
        result.addAll(callbacks);

        return result;
    }

    private AlarmCallbackConfigurationAVImpl implOrFail(AlarmCallbackConfiguration callback) {
        final AlarmCallbackConfigurationAVImpl callbackImpl;
        if (callback instanceof AlarmCallbackConfigurationAVImpl) {
            callbackImpl = (AlarmCallbackConfigurationAVImpl) callback;
            return callbackImpl;
        } else {
            throw new IllegalArgumentException("Supplied output must be of implementation type AlarmCallbackConfigurationAVImpl, not " + callback.getClass());
        }
    }
}
