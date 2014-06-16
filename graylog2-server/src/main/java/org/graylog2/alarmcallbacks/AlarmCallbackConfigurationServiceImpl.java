/*
 * Copyright 2012-2014 TORCH GmbH
 *
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

package org.graylog2.alarmcallbacks;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class AlarmCallbackConfigurationServiceImpl extends PersistedServiceImpl implements AlarmCallbackConfigurationService {
    @Inject
    public AlarmCallbackConfigurationServiceImpl(MongoConnection mongoConnection) {
        super(mongoConnection);
    }

    @Override
    public List<AlarmCallbackConfiguration> getForStreamId(String streamId) {
        final List<AlarmCallbackConfiguration> alarmCallbackConfigurations = Lists.newArrayList();
        final List<DBObject> respConfigurations = query(AlarmCallbackConfigurationImpl.class,
                new BasicDBObject("stream_id", streamId)
        );

        for (DBObject configuration : respConfigurations) {
            alarmCallbackConfigurations.add(new AlarmCallbackConfigurationImpl((ObjectId)configuration.get("_id"), configuration.toMap()));
        }

        return alarmCallbackConfigurations;
    }

    @Override
    public List<AlarmCallbackConfiguration> getForStream(Stream stream) {
        return getForStreamId(stream.getId());
    }

    @Override
    public AlarmCallbackConfiguration load(String alarmCallbackId) {
        DBObject rawModel = get(AlarmCallbackConfigurationImpl.class, alarmCallbackId);
        return (rawModel == null ? null : new AlarmCallbackConfigurationImpl((ObjectId)(rawModel.get("_id")), rawModel.toMap()));
    }

    @Override
    public AlarmCallbackConfiguration create(String streamId, CreateAlarmCallbackRequest request) {
        Map<String, Object> fields = Maps.newHashMap();
        fields.put("stream_id", new ObjectId(streamId));
        fields.put("type", request.type);
        fields.put("configuration", request.configuration);
        fields.put("created_at", DateTime.now());
        fields.put("creator_user_id", request.creatorUserId);

        return new AlarmCallbackConfigurationImpl(fields);
    }
}
