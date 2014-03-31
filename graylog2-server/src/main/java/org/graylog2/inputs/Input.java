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
import com.google.common.collect.Maps;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.ConfigurationException;
import org.graylog2.Core;
import org.graylog2.cluster.Node;
import org.graylog2.database.Persisted;
import org.graylog2.database.ValidationException;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.MapValidator;
import org.graylog2.database.validators.Validator;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.users.User;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Input extends Persisted {

    private static final Logger LOG = LoggerFactory.getLogger(Input.class);

    public static final String COLLECTION = "inputs";

    public static final String EMBEDDED_EXTRACTORS = "extractors";
    public static final String EMBEDDED_STATIC_FIELDS = "static_fields";

    public Input(Core core, Map<String, Object> fields) {
        super(core, fields);
    }

    public Input(Core core, ObjectId id, Map<String, Object> fields) {
        super(core, id, fields);
    }

    public static List<Input> allOfThisNode(Core core) {
        List<Input> inputs = Lists.newArrayList();
        List<BasicDBObject> query = new ArrayList<BasicDBObject>();
        query.add(new BasicDBObject("node_id", core.getNodeId()));
        query.add(new BasicDBObject("global", true));
        List<DBObject> ownInputs = query(new BasicDBObject( "$or", query), core, COLLECTION);
        for (DBObject o : ownInputs) {
            inputs.add(new Input(core, (ObjectId) o.get("_id"), o.toMap()));
        }

        return inputs;
    }

    public static List<Input> allOfRadio(Core core, Node radio) {
        List<Input> inputs = Lists.newArrayList();
        List<BasicDBObject> query = Lists.newArrayList();
        query.add(new BasicDBObject("radio_id", radio.getNodeId()));
        query.add(new BasicDBObject("global", true));

        for (DBObject o : query(new BasicDBObject("$or", query), core, COLLECTION)) {
            inputs.add(new Input(core, (ObjectId) o.get("_id"), o.toMap()));
        }

        return inputs;
    }

    public static Input find(Core core, String id) {
        DBObject o = findOne(new BasicDBObject("_id", new ObjectId(id)), core, COLLECTION);
        return new Input(core, (ObjectId) o.get("_id"), o.toMap());
    }

    public static Input findForThisNode(Core core, String id) {
        List<BasicDBObject> query = new ArrayList<BasicDBObject>();
        query.add(new BasicDBObject("_id", new ObjectId(id)));

        List<BasicDBObject> forThisNodeOrGlobal = new ArrayList<BasicDBObject>();
        forThisNodeOrGlobal.add(new BasicDBObject("node_id", core.getNodeId()));
        forThisNodeOrGlobal.add(new BasicDBObject("global", true));

        query.add(new BasicDBObject("$or", forThisNodeOrGlobal));

        DBObject o = findOne(new BasicDBObject("$and", query), core, COLLECTION);

        return new Input(core, (ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public String getCollectionName() {
        return COLLECTION;
    }

    @Override
    protected Map<String, Validator> getValidations() {
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
    protected Map<String, Validator> getEmbeddedValidations(String key) {
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

    public void addExtractor(Extractor extractor) throws ValidationException {
        embed(EMBEDDED_EXTRACTORS, extractor);
    }

    public void addStaticField(final String key, final String value) throws ValidationException {
        EmbeddedPersistable obj = new EmbeddedPersistable() {
            @Override
            public Map<String, Object> getPersistedFields() {
                return new HashMap<String, Object>() {{
                    put("key", key);
                    put("value", value);
                }};
            }
        };

        embed(EMBEDDED_STATIC_FIELDS, obj);
    }

    public void removeExtractor(String extractorId) {
        removeEmbedded(EMBEDDED_EXTRACTORS, extractorId);
    }

    public void removeStaticField(String key) {
        removeEmbedded("key", EMBEDDED_STATIC_FIELDS, key);
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

    public List<Extractor> getExtractors() {
        List<Extractor> extractors = Lists.newArrayList();

        if (fields.get(EMBEDDED_EXTRACTORS) == null) {
            return extractors;
        }

        BasicDBList mEx = (BasicDBList) fields.get(EMBEDDED_EXTRACTORS);
        Iterator<Object> iterator = mEx.iterator();
        while(iterator.hasNext()) {
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

    public Map<String, String> getStaticFields() {
        Map<String, String> staticFields = Maps.newHashMap();

        if (fields.get(EMBEDDED_STATIC_FIELDS) == null) {
            return staticFields;
        }

        BasicDBList list = (BasicDBList) fields.get(EMBEDDED_STATIC_FIELDS);
        Iterator<Object> iterator = list.iterator();
        while(iterator.hasNext()) {
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

    public String getType() {
        return (String) fields.get("type");
    }

    public String getCreatorUserId() {
        return (String) fields.get("creator_user_id");
    }

    public String getInputId() {
        return (String) fields.get("input_id");
    }

    private List<Converter> getConvertersOfExtractor(DBObject extractor) {
        List<Converter> cl = Lists.newArrayList();

        BasicDBList m = (BasicDBList) extractor.get("converters");
        Iterator<Object> iterator = m.iterator();
        while(iterator.hasNext()) {
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

    public Boolean isGlobal() {
        Object global = fields.get("global");
        if (global != null && global instanceof Boolean)
            return (Boolean) global;
        else
            return false;
    }

}
