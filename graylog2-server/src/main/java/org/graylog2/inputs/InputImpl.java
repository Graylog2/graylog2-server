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
package org.graylog2.inputs;

import com.google.common.collect.Maps;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.MapValidator;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@CollectionName("inputs")
public class InputImpl extends PersistedImpl implements Input {

    public static final String EMBEDDED_EXTRACTORS = "extractors";
    public static final String EMBEDDED_STATIC_FIELDS = "static_fields";
    public static final String EMBEDDED_STATIC_FIELDS_KEY = "key";
    private static final Logger LOG = LoggerFactory.getLogger(InputImpl.class);

    public InputImpl(Map<String, Object> fields) {
        super(fields);
    }

    public InputImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public Map<String, Validator> getValidations() {
        return new HashMap<String, Validator>() {{
            put("input_id", new FilledStringValidator());
            put("title", new FilledStringValidator());
            put("type", new FilledStringValidator());
            put("configuration", new MapValidator());
            put("creator_user_id", new FilledStringValidator());
            put("created_at", new DateValidator());
        }};
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        if (key.equals(EMBEDDED_EXTRACTORS)) {
            return new HashMap<String, Validator>() {{
                put("id", new FilledStringValidator());
                put("title", new FilledStringValidator());
                put("type", new FilledStringValidator());
                put("cursor_strategy", new FilledStringValidator());
                put("target_field", new FilledStringValidator());
                put("source_field", new FilledStringValidator());
                put("creator_user_id", new FilledStringValidator());
                put("extractor_config", new MapValidator());
            }};
        }

        if (key.equals(EMBEDDED_STATIC_FIELDS)) {
            return new HashMap<String, Validator>() {{
                put("key", new FilledStringValidator());
                put("value", new FilledStringValidator());
            }};
        }

        return Maps.newHashMap();
    }

    @Override
    public String getTitle() {
        return (String) fields.get("title");
    }

    @Override
    public DateTime getCreatedAt() {
        return new DateTime(fields.get("created_at"));
    }

    @Override
    public Map<String, Object> getConfiguration() {
        return (Map<String, Object>) fields.get("configuration");
    }

    @Override
    public Map<String, String> getStaticFields() {
        Map<String, String> staticFields = Maps.newHashMap();

        if (fields.get(EMBEDDED_STATIC_FIELDS) == null) {
            return staticFields;
        }

        BasicDBList list = (BasicDBList) fields.get(EMBEDDED_STATIC_FIELDS);
        Iterator<Object> iterator = list.iterator();
        while (iterator.hasNext()) {
            try {
                DBObject field = (BasicDBObject) iterator.next();
                staticFields.put((String) field.get("key"), (String) field.get("value"));
            } catch (Exception e) {
                LOG.error("Cannot build static field from persisted data. Skipping.", e);
                continue;
            }
        }

        return staticFields;
    }

    @Override
    public String getType() {
        return (String) fields.get("type");
    }

    @Override
    public String getCreatorUserId() {
        return (String) fields.get("creator_user_id");
    }

    @Override
    public String getInputId() {
        return (String) fields.get("input_id");
    }

    @Override
    public Boolean isGlobal() {
        Object global = fields.get("global");
        if (global != null && global instanceof Boolean)
            return (Boolean) global;
        else
            return false;
    }

}
