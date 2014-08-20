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

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.validators.ClassNameStringValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.outputs.MessageOutput;
import org.graylog2.plugin.streams.Output;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@CollectionName("outputs")
public class OutputImpl implements Output {
    private ObjectId _id;
    private String title;
    private String type;
    private Map<String, Object> configuration;
    @JsonProperty("created_at")
    private Date createdAt;
    @JsonProperty("creator_user_id")
    private String creatorUserId;

    public OutputImpl() {
        this._id = new ObjectId();
    }

    public OutputImpl(String title, String type, Map<String, Object> configuration, Date createdAt, String creatorUserId) {
        this._id = new ObjectId();
        this.title = title;
        this.type = type;
        this.configuration = configuration;
        this.createdAt = createdAt;
        this.creatorUserId = creatorUserId;
    }

    public OutputImpl(ObjectId id, Map<String, Object> fields) {
        this._id = id;
        this.title = fields.get("title").toString();
        this.type = fields.get("type").toString();
        this.configuration = (Map<String, Object>)fields.get("configuration");
        this.createdAt = (Date)fields.get("created_at");
        this.creatorUserId = fields.get("creator_user_id").toString();
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
        return new HashMap<String, Validator>() {{
            put("title", new FilledStringValidator());
            put("type", new ClassNameStringValidator(MessageOutput.class));
            put("creator_user_id", new FilledStringValidator());
        }};
    }

    public Map<String, Validator> getEmbeddedValidations(String key) {
        return new HashMap<>();
    }

    @Override
    public String getId() {
        return _id.toStringMongod();
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

    @Override
    public Map<String, Object> getFields() {
        return new HashMap<String, Object>() {{
            put("_id", new ObjectId(getId()));
            put("title", getTitle());
            put("type", getType());
            put("configuration", getConfiguration());
            put("creator_user_id", getCreatorUserId());
            put("created_at", getCreatedAt());
        }};
    }

    @Override
    @JsonValue
    public Map<String, Object> asMap() {
        return getFields();
    }
}
