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
package org.graylog2.alarmcallbacks;

import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@CollectionName("alarmcallbackconfigurations")
public class AlarmCallbackConfigurationImpl extends PersistedImpl implements AlarmCallbackConfiguration {
    private ObjectId stream_id;
    private String type;
    private Configuration configuration;
    private DateTime createdAt;
    private String creatorUserId;

    public AlarmCallbackConfigurationImpl(Map<String, Object> fields) {
        this(new ObjectId(), fields);
    }

    public AlarmCallbackConfigurationImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
        if (fields.get("stream_id") != null) {
            Object rawStreamId = fields.get("stream_id");
            if (rawStreamId instanceof String && !(((String)rawStreamId).isEmpty()))
                this.stream_id = new ObjectId((String)rawStreamId);
            if (rawStreamId instanceof ObjectId)
                this.stream_id = (ObjectId)rawStreamId;
        }

        this.type = (String)fields.get("type");
        this.configuration = new Configuration((Map<String, Object>)fields.get("configuration"));
        this.createdAt = (DateTime)fields.get("created_at");
        this.creatorUserId = (String) fields.get("creator_user_id");
    }

    public String getStreamId() {
        return stream_id.toHexString();
    }

    public String getType() {
        return type;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setStream(Stream stream) {
        setStreamId(stream.getId());
    }

    public void setStreamId(String stream_id) {
        this.stream_id = new ObjectId(stream_id);
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    public DateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(DateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    public void setCreatorUserId(String creatorUserId) {
        this.creatorUserId = creatorUserId;
    }

    @Override
    public Map<String, Validator> getValidations() {
        Map<String, Validator> result = Maps.newHashMap();
        return result;
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        Map<String, Validator> result = Maps.newHashMap();
        return result;
    }

    @Override
    public Map<String, Object> getFields() {
        Map<String, Object> result = Maps.newHashMap();
        result.put("id", getId());
        result.put("stream_id", getStreamId());
        result.put("type", getType());
        result.put("configuration", getConfiguration().getSource());
        result.put("created_at", getCreatedAt());
        result.put("creator_user_id", getCreatorUserId());

        return result;
    }
}
