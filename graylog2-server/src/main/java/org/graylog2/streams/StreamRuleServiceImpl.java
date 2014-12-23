/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.streams;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.plugin.streams.StreamRule;
import org.graylog2.rest.resources.streams.rules.requests.CreateStreamRuleRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StreamRuleServiceImpl extends PersistedServiceImpl implements StreamRuleService {
    @Inject
    public StreamRuleServiceImpl(MongoConnection mongoConnection) {
        super(mongoConnection);
    }

    @Override
    public StreamRule load(ObjectId id) throws NotFoundException {
        BasicDBObject o = (BasicDBObject) get(StreamRuleImpl.class, id);

        if (o == null) {
            throw new NotFoundException();
        }

        return new StreamRuleImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public List<StreamRule> loadForStream(Stream stream) throws NotFoundException {
        return loadForStreamId(stream.getId());
    }

    @Override
    public StreamRule create(Map<String, Object> data) {
        return new StreamRuleImpl(data);
    }

    @Override
    public StreamRule create(String streamid, CreateStreamRuleRequest cr) {
        Map<String, Object> streamRuleData = Maps.newHashMap();
        streamRuleData.put(StreamRuleImpl.FIELD_TYPE, cr.type);
        streamRuleData.put(StreamRuleImpl.FIELD_VALUE, cr.value);
        streamRuleData.put(StreamRuleImpl.FIELD_FIELD, cr.field);
        streamRuleData.put(StreamRuleImpl.FIELD_INVERTED, cr.inverted);
        streamRuleData.put(StreamRuleImpl.FIELD_STREAM_ID, new ObjectId(streamid));

        return new StreamRuleImpl(streamRuleData);
    }

    @Override
    public List<StreamRule> loadForStreamId(String streamId) throws NotFoundException {
        ObjectId id = new ObjectId(streamId);
        final List<StreamRule> streamRules = new ArrayList<StreamRule>();
        final List<DBObject> respStreamRules = query(StreamRuleImpl.class,
                new BasicDBObject(StreamRuleImpl.FIELD_STREAM_ID, id)
        );

        for (DBObject streamRule : respStreamRules) {
            streamRules.add(load((ObjectId) streamRule.get("_id")));
        }

        return streamRules;
    }
}