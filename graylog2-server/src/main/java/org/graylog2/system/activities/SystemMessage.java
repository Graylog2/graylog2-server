/**
 * Copyright 2013 Lennart Koopmann <lennart@torch.sh>
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
package org.graylog2.system.activities;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.Persisted;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.Validator;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class SystemMessage extends Persisted {

    public static final String COLLECTION = "system_messages";
    private static final int PER_PAGE = 30;

    private final String caller;
    private final String content;
    private final DateTime timestamp;
    private final String nodeId;

    public SystemMessage(Map<String, Object> fields, Core core) {
        super(core, fields);

        this.caller = (String) fields.get("caller");
        this.content = (String) fields.get("content");
        this.timestamp = (DateTime) fields.get("timestamp");
        this.nodeId = (String) fields.get("node_id");
    }

    protected SystemMessage(ObjectId id, Map<String, Object> fields, Core core) {
        super(core, id, fields);

        this.caller = (String) fields.get("caller");
        this.content = (String) fields.get("content");
        this.timestamp = new DateTime(fields.get("timestamp"));
        this.nodeId = (String) fields.get("node_id");
    }

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    public static List<SystemMessage> all(Core core, int page) {
        List<SystemMessage> messages = Lists.newArrayList();

        DBObject sort = new BasicDBObject();
        sort.put("timestamp", -1);

        List<DBObject> results = query(new BasicDBObject(), sort, PER_PAGE, PER_PAGE*page, core, COLLECTION);
        for (DBObject o : results) {
            messages.add(new SystemMessage((ObjectId) o.get("_id"), o.toMap(), core));
        }

        return messages;
    }

    public String getCaller() {
        return caller;
    }

    public String getContent() {
        return content;
    }

    public DateTime getTimestamp() {
        return timestamp;
    }

    public String getNodeId() {
        return nodeId;
    }

    @Override
    public ObjectId getId() {
        return this.id;
    }

    @Override
    protected Map<String, Validator> getValidations() {
        return new HashMap<String, Validator>() {{
            put("caller", new FilledStringValidator());
            put("content", new FilledStringValidator());
            put("node_id", new FilledStringValidator());
            put("timestamp", new DateValidator());
        }};
    }

    @Override
    protected Map<String, Validator> getEmbeddedValidations(String key) {
        return Maps.newHashMap();
    }

}