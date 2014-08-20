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
package org.graylog2.inputs;

import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mongodb.*;
import org.bson.types.ObjectId;
import org.graylog2.cluster.Node;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.database.ValidationException;
import org.graylog2.inputs.converters.ConverterFactory;
import org.graylog2.inputs.extractors.ExtractorFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.plugin.inputs.Converter;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.MessageInputFactory;
import org.graylog2.shared.inputs.NoSuchInputTypeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class InputServiceImpl extends PersistedServiceImpl implements InputService {
    private static final Logger LOG = LoggerFactory.getLogger(InputServiceImpl.class);

    private final ExtractorFactory extractorFactory;
    private final MessageInputFactory messageInputFactory;

    @Inject
    public InputServiceImpl(MongoConnection mongoConnection,
                            ExtractorFactory extractorFactory,
                            MessageInputFactory messageInputFactory) {
        super(mongoConnection);
        this.extractorFactory = extractorFactory;
        this.messageInputFactory = messageInputFactory;
    }

    @Override
    public List<Input> allOfThisNode(String nodeId) {
        List<Input> inputs = Lists.newArrayList();
        List<BasicDBObject> query = new ArrayList<BasicDBObject>();
        query.add(new BasicDBObject("node_id", nodeId));
        query.add(new BasicDBObject("global", true));
        List<DBObject> ownInputs = query(org.graylog2.inputs.InputImpl.class, new BasicDBObject("$or", query));
        for (DBObject o : ownInputs) {
            inputs.add(new org.graylog2.inputs.InputImpl((ObjectId) o.get("_id"), o.toMap()));
        }

        return inputs;
    }

    @Override
    public List<Input> allOfRadio(Node radio) {
        List<Input> inputs = Lists.newArrayList();
        List<BasicDBObject> query = Lists.newArrayList();
        query.add(new BasicDBObject("radio_id", radio.getNodeId()));
        query.add(new BasicDBObject("global", true));

        for (DBObject o : query(InputImpl.class, new BasicDBObject("$or", query))) {
            inputs.add(new org.graylog2.inputs.InputImpl((ObjectId) o.get("_id"), o.toMap()));
        }

        return inputs;
    }

    @Override
    public Input find(String id) throws NotFoundException {
        DBObject o = get(org.graylog2.inputs.InputImpl.class, id);
        if (o == null)
            throw new NotFoundException("Input <" + id + "> not found!");
        return new org.graylog2.inputs.InputImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public Input findForThisNodeOrGlobal(String nodeId, String id) throws NotFoundException {
        List<BasicDBObject> query = new ArrayList<BasicDBObject>();
        query.add(new BasicDBObject("_id", new ObjectId(id)));

        List<BasicDBObject> forThisNodeOrGlobal = new ArrayList<BasicDBObject>();
        forThisNodeOrGlobal.add(new BasicDBObject("node_id", nodeId));
        forThisNodeOrGlobal.add(new BasicDBObject("global", true));

        query.add(new BasicDBObject("$or", forThisNodeOrGlobal));

        DBObject o = findOne(InputImpl.class, new BasicDBObject("$and", query));

        return new InputImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public Input findForThisRadioOrGlobal(final String radioId, String id) throws NotFoundException {
        List<DBObject> query = new ArrayList<>();
        query.add(new BasicDBObject("_id", new ObjectId(id)));
        List<DBObject> radioIdOrGlobal = new ArrayList<DBObject>() {{
            add(new BasicDBObject("radio_id", radioId));
            add(new BasicDBObject("global", true));
        }};

        query.add(new BasicDBObject("$or", radioIdOrGlobal));

        DBObject o = findOne(InputImpl.class, new BasicDBObject("$and", query));

        if (o == null)
            throw new NotFoundException();
        else
            return new InputImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public Input findForThisNode(String nodeId, String id) throws NotFoundException, IllegalArgumentException {
        List<BasicDBObject> query = new ArrayList<BasicDBObject>();
        query.add(new BasicDBObject("_id", new ObjectId(id)));

        List<BasicDBObject> forThisNode = new ArrayList<BasicDBObject>();
        forThisNode.add(new BasicDBObject("node_id", nodeId));
        forThisNode.add(new BasicDBObject("global", false));

        query.add(new BasicDBObject("$and", forThisNode));

        DBObject o = findOne(InputImpl.class, new BasicDBObject("$and", query));

        if (o == null)
            throw new NotFoundException();
        else
            return new InputImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public Input findForThisRadio(String radioId, String id) throws NotFoundException {
        List<DBObject> query = new ArrayList<>();
        query.add(new BasicDBObject("_id", new ObjectId(id)));
        query.add(new BasicDBObject("radio_id", radioId));
        List<Object> list = new ArrayList<Object>()
        {{
            add(false);
            add(null);
        }};
        query.add(QueryBuilder.start("global").in(list).get());

        DBObject o = findOne(InputImpl.class, new BasicDBObject("$and", query));

        if (o == null)
            throw new NotFoundException();
        else
            return new InputImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public void addExtractor(Input input, Extractor extractor) throws ValidationException {
        embed(input, InputImpl.EMBEDDED_EXTRACTORS, extractor);
    }

    @Override
    public void addStaticField(Input input, final String key, final String value) throws ValidationException {
        EmbeddedPersistable obj = new EmbeddedPersistable() {
            @Override
            public Map<String, Object> getPersistedFields() {
                return new HashMap<String, Object>() {{
                    put("key", key);
                    put("value", value);
                }};
            }
        };

        embed(input, InputImpl.EMBEDDED_STATIC_FIELDS, obj);
    }

    @Override
    public List<Extractor> getExtractors(Input input) {
        List<Extractor> extractors = Lists.newArrayList();

        if (input.getFields().get(InputImpl.EMBEDDED_EXTRACTORS) == null) {
            return extractors;
        }

        BasicDBList mEx = (BasicDBList) input.getFields().get(InputImpl.EMBEDDED_EXTRACTORS);
        Iterator<Object> iterator = mEx.iterator();
        while (iterator.hasNext()) {
            DBObject ex = (BasicDBObject) iterator.next();

            // SOFT MIGRATION: does this extractor have an order set? Implemented for issue: #726
            Long order = new Long(0);
            if (ex.containsField("order")) {
                order = (Long) ex.get("order"); // mongodb driver gives us a java.lang.Long
            }

            try {
                Extractor extractor = extractorFactory.factory(
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
    public void removeExtractor(Input input, String extractorId) {
        removeEmbedded(input, InputImpl.EMBEDDED_EXTRACTORS, extractorId);
    }

    @Override
    public void removeStaticField(Input input, String key) {
        removeEmbedded(input, InputImpl.EMBEDDED_STATIC_FIELDS_KEY, InputImpl.EMBEDDED_STATIC_FIELDS, key);
    }

    @Override
    public MessageInput buildMessageInput(Input io) throws NoSuchInputTypeException {
        MessageInput input = messageInputFactory.create(io.getType());

        // Add all standard fields.
        input.setTitle(io.getTitle());
        input.setCreatorUserId(io.getCreatorUserId());
        input.setPersistId(io.getId());
        input.setCreatedAt(io.getCreatedAt());
        if (io.isGlobal())
            input.setGlobal(true);

        // Add extractors.
        for (Extractor extractor : this.getExtractors(io)) {
            input.addExtractor(extractor.getId(), extractor);
        }

        // Add static fields.
        for (Map.Entry<String, String> field : io.getStaticFields().entrySet()) {
            input.addStaticField(field.getKey(), field.getValue());
        }

        return input;
    }

    @Override
    public MessageInput getMessageInput(Input io) throws NoSuchInputTypeException {
        MessageInput input = buildMessageInput(io);
        input.initialize(new Configuration(io.getConfiguration()));

        return input;
    }
}