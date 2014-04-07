/*
 * Copyright 2013-2014 TORCH GmbH
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

import com.google.common.collect.Lists;
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
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
@CollectionName("inputs")
public class InputImpl extends PersistedImpl implements Input {

    public static final String EMBEDDED_EXTRACTORS = "extractors";
    public static final String EMBEDDED_STATIC_FIELDS = "static_fields";
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
    public List<Extractor> getExtractors() {
        List<Extractor> extractors = Lists.newArrayList();

        if (fields.get(EMBEDDED_EXTRACTORS) == null) {
            return extractors;
        }

        BasicDBList mEx = (BasicDBList) fields.get(EMBEDDED_EXTRACTORS);
        Iterator<Object> iterator = mEx.iterator();
        while (iterator.hasNext()) {
            DBObject ex = (BasicDBObject) iterator.next();

            // SOFT MIGRATION: does this extractor have an order set? Implemented for issue: #726
            Long order = new Long(0);
            if (ex.containsField("order")) {
                order = (Long) ex.get("order"); // mongodb driver gives us a java.lang.Long
            }

            try {
                Extractor extractor = ExtractorFactory.factory(
                        (String) ex.get("id"),
                        (String) ex.get("title"),
                        order.intValue(),
                        Extractor.CursorStrategy.valueOf(((String) ex.get("cursor_strategy")).toUpperCase()),
                        Extractor.Type.valueOf(((String) ex.get("type")).toUpperCase()),
                        (String) ex.get("source_field"),
                        (String) ex.get("target_field"),
                        (Map<String, Object>) ex.get("extractor_config"),
                        (String) ex.get("creator_user_id"),
                        getConvertersOfExtractor(ex),
                        Extractor.ConditionType.valueOf(((String) ex.get("condition_type")).toUpperCase()),
                        (String) ex.get("condition_value")
                );

                extractors.add(extractor);
            } catch (Exception e) {
                LOG.error("Cannot build extractor from persisted data. Skipping.", e);
                continue;
            }
        }

        return extractors;
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

    private List<Converter> getConvertersOfExtractor(DBObject extractor) {
        List<Converter> cl = Lists.newArrayList();

        BasicDBList m = (BasicDBList) extractor.get("converters");
        Iterator<Object> iterator = m.iterator();
        while (iterator.hasNext()) {
            DBObject c = (BasicDBObject) iterator.next();

            try {
                cl.add(ConverterFactory.factory(
                        Converter.Type.valueOf(((String) c.get("type")).toUpperCase()),
                        (Map<String, Object>) c.get("config")
                ));
            } catch (ConverterFactory.NoSuchConverterException e1) {
                LOG.error("Cannot build converter from persisted data. No such converter.", e1);
                continue;
            } catch (Exception e) {
                LOG.error("Cannot build converter from persisted data.", e);
                continue;
            }
        }

        return cl;
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
