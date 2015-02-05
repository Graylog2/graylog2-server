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
package org.graylog2.streams;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.google.common.collect.ImmutableMap;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.OptionalStringValidator;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.streams.Output;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@CollectionName("outputs")
public class OutputImpl implements Output {
    static final String FIELD_ID = "_id";
    static final String FIELD_TITLE = "title";
    static final String FIELD_TYPE = "type";
    static final String FIELD_CONFIGURATION = "configuration";
    static final String FIELD_CREATOR_USER_ID = "creator_user_id";
    static final String FIELD_CREATED_AT = "created_at";
    static final String FIELD_CONTENT_PACK = "content_pack";

    private ObjectId _id;
    private String title;
    private String type;
    private Map<String, Object> configuration;
    @JsonProperty(FIELD_CREATED_AT)
    private Date createdAt;
    @JsonProperty(FIELD_CREATOR_USER_ID)
    private String creatorUserId;
    @Nullable
    private String contentPack = null;

    public OutputImpl() {
        this._id = new ObjectId();
    }

    public OutputImpl(String title, String type, Map<String, Object> configuration, Date createdAt, String creatorUserId) {
        this(title, type, configuration, createdAt, creatorUserId, null);
    }

    public OutputImpl(String title, String type, Map<String, Object> configuration, Date createdAt, String creatorUserId,
                      @Nullable String contentPack) {
        this._id = new ObjectId();
        this.title = title;
        this.type = type;
        this.configuration = configuration;
        this.createdAt = createdAt;
        this.creatorUserId = creatorUserId;
        this.contentPack = contentPack;
    }

    public OutputImpl(ObjectId id, Map<String, Object> fields) {
        this._id = id;
        this.title = (String) fields.get(FIELD_TITLE);
        this.type = (String) fields.get(FIELD_TYPE);
        this.configuration = (Map<String, Object>) fields.get(FIELD_CONFIGURATION);
        this.createdAt = (Date) fields.get(FIELD_CREATED_AT);
        this.creatorUserId = (String) fields.get(FIELD_CREATOR_USER_ID);
        this.contentPack = (String) fields.get(FIELD_CONTENT_PACK);
    }

    public String getTitle() {
        return title;
    }

    public String getType() {
        return type;
    }

    public Map<String, Object> getConfiguration() {
        return configuration;
    }

    public Map<String, Validator> getValidations() {
        return ImmutableMap.<String, Validator>of(
                FIELD_TITLE, new FilledStringValidator(),
                FIELD_TYPE, new FilledStringValidator(),
                FIELD_CREATOR_USER_ID, new FilledStringValidator(),
                FIELD_CONTENT_PACK, new OptionalStringValidator());
    }

    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Collections.emptyMap();
    }

    @Override
    public String getId() {
        return _id.toHexString();
    }

    public void setId(String id) {
        this._id = new ObjectId(id);
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public String getCreatorUserId() {
        return creatorUserId;
    }

    @Nullable
    public String getContentPack() {
        return contentPack;
    }

    @Override
    public Map<String, Object> getFields() {
        final HashMap<String, Object> fields = new HashMap<>();
        fields.put(FIELD_ID, new ObjectId(getId()));
        fields.put(FIELD_TITLE, getTitle());
        fields.put(FIELD_TYPE, getType());
        fields.put(FIELD_CONFIGURATION, getConfiguration());
        fields.put(FIELD_CREATOR_USER_ID, getCreatorUserId());
        fields.put(FIELD_CREATED_AT, getCreatedAt());
        fields.put(FIELD_CONTENT_PACK, getContentPack());

        return fields;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OutputImpl)) return false;

        OutputImpl output = (OutputImpl) o;

        if (!_id.equals(output._id)) return false;
        if (configuration != null ? !configuration.equals(output.configuration) : output.configuration != null)
            return false;
        if (contentPack != null ? !contentPack.equals(output.contentPack) : output.contentPack != null) return false;
        if (!createdAt.equals(output.createdAt)) return false;
        if (!creatorUserId.equals(output.creatorUserId)) return false;
        if (!title.equals(output.title)) return false;
        if (!type.equals(output.type)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = _id.hashCode();
        result = 31 * result + title.hashCode();
        result = 31 * result + type.hashCode();
        result = 31 * result + (configuration != null ? configuration.hashCode() : 0);
        result = 31 * result + createdAt.hashCode();
        result = 31 * result + creatorUserId.hashCode();
        result = 31 * result + (contentPack != null ? contentPack.hashCode() : 0);
        return result;
    }

    @Override
    @JsonValue
    public Map<String, Object> asMap() {
        final Map<String, Object> fields = getFields();
        fields.put("id", ((ObjectId) fields.remove(FIELD_ID)).toHexString());
        return fields;
    }
}
