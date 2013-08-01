/**
 * Copyright 2013 Lennart Koopmann <lennart@socketfeed.com>
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
 *
 */
package org.graylog2.inputs;

import com.google.common.collect.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.Persisted;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.MapValidator;
import org.graylog2.database.validators.Validator;
import org.graylog2.users.User;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Input extends Persisted {

    private static final Logger LOG = LoggerFactory.getLogger(Input.class);

    public static final String COLLECTION = "inputs";

    public Input(Core core, Map<String, Object> fields) {
        super(core, fields);
    }

    public Input(Core core, ObjectId id, Map<String, Object> fields) {
        super(core, id, fields);
    }

    public static List<Input> allOfThisNode(Core core) {
        List<Input> inputs = Lists.newArrayList();
        for (DBObject o : query(new BasicDBObject("node_id", core.getNodeId()), core, COLLECTION)) {
            inputs.add(new Input(core, (ObjectId) o.get("_id"), o.toMap()));
        }

        return inputs;
    }

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    @Override
    protected Map<String, Validator> getValidations() {
        return new HashMap<String, Validator>() {{
            put("title", new FilledStringValidator());
            put("type", new FilledStringValidator());
            put("configuration", new MapValidator());
            put("creator_user_id", new FilledStringValidator());
            put("created_at", new DateValidator());
        }};
    }

    public String getTitle() {
        return (String) fields.get("title");
    }

    public DateTime getCreatedAt() {
        return new DateTime(fields.get("created_at"));
    }

    public Map<String, Object> getConfiguration() {
        return (Map<String, Object>) fields.get("configuration");
    }

    public String getType() {
        return (String) fields.get("type");
    }

    public String getCreatorUserId() {
        return (String) fields.get("creator_user_id");
    }

}
