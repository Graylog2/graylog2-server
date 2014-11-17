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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Inject;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.QueryBuilder;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    public List<Input> allOfThisNode(final String nodeId) {
        final List<BasicDBObject> query = ImmutableList.of(
                new BasicDBObject(MessageInput.FIELD_NODE_ID, nodeId),
                new BasicDBObject(MessageInput.FIELD_GLOBAL, true));
        final List<DBObject> ownInputs = query(InputImpl.class, new BasicDBObject("$or", query));

        final ImmutableList.Builder<Input> inputs = ImmutableList.builder();
        for (final DBObject o : ownInputs) {
            inputs.add(new InputImpl((ObjectId) o.get("_id"), o.toMap()));
        }

        return inputs.build();
    }

    @Override
    public List<Input> allOfRadio(Node radio) {
        final List<BasicDBObject> query = ImmutableList.of(
                new BasicDBObject(MessageInput.FIELD_RADIO_ID, radio.getNodeId()),
                new BasicDBObject(MessageInput.FIELD_GLOBAL, true));

        final ImmutableList.Builder<Input> inputs = ImmutableList.builder();
        for (DBObject o : query(InputImpl.class, new BasicDBObject("$or", query))) {
            inputs.add(new InputImpl((ObjectId) o.get("_id"), o.toMap()));
        }

        return inputs.build();
    }

    @Override
    public Input create(Map<String, Object> fields) {
        return new InputImpl(fields);
    }

    @Override
    public Input find(String id) throws NotFoundException {
        final DBObject o = get(org.graylog2.inputs.InputImpl.class, id);
        if (o == null) {
            throw new NotFoundException("Input <" + id + "> not found!");
        }
        return new org.graylog2.inputs.InputImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public Input findForThisNodeOrGlobal(String nodeId, String id) throws NotFoundException {
        final List<BasicDBObject> forThisNodeOrGlobal = ImmutableList.of(
                new BasicDBObject(MessageInput.FIELD_NODE_ID, nodeId),
                new BasicDBObject(MessageInput.FIELD_GLOBAL, true));

        final List<BasicDBObject> query = ImmutableList.of(
                new BasicDBObject("_id", new ObjectId(id)),
                new BasicDBObject("$or", forThisNodeOrGlobal));

        final DBObject o = findOne(InputImpl.class, new BasicDBObject("$and", query));
        return new InputImpl((ObjectId) o.get("_id"), o.toMap());
    }

    @Override
    public Input findForThisRadioOrGlobal(final String radioId, String id) throws NotFoundException {
        final List<DBObject> radioIdOrGlobal = ImmutableList.<DBObject>of(
                new BasicDBObject(MessageInput.FIELD_RADIO_ID, radioId),
                new BasicDBObject(MessageInput.FIELD_GLOBAL, true));

        final List<DBObject> query = ImmutableList.<DBObject>of(
                new BasicDBObject("_id", new ObjectId(id)),
                new BasicDBObject("$or", radioIdOrGlobal));

        final DBObject o = findOne(InputImpl.class, new BasicDBObject("$and", query));
        if (o == null) {
            throw new NotFoundException();
        } else {
            return new InputImpl((ObjectId) o.get("_id"), o.toMap());
        }
    }

    @Override
    public Input findForThisNode(String nodeId, String id) throws NotFoundException, IllegalArgumentException {
        final List<BasicDBObject> forThisNode = ImmutableList.of(
                new BasicDBObject(MessageInput.FIELD_NODE_ID, nodeId),
                new BasicDBObject(MessageInput.FIELD_GLOBAL, false));

        final List<BasicDBObject> query = ImmutableList.of(
                new BasicDBObject("_id", new ObjectId(id)),
                new BasicDBObject("$and", forThisNode));

        final DBObject o = findOne(InputImpl.class, new BasicDBObject("$and", query));
        if (o == null) {
            throw new NotFoundException();
        } else {
            return new InputImpl((ObjectId) o.get("_id"), o.toMap());
        }
    }

    @Override
    public Input findForThisRadio(String radioId, String id) throws NotFoundException {
        final List<Object> list = new ArrayList<Object>() {{
            add(false);
            add(null);
        }};

        final List<DBObject> query = ImmutableList.of(
                new BasicDBObject("_id", new ObjectId(id)),
                new BasicDBObject(MessageInput.FIELD_RADIO_ID, radioId),
                QueryBuilder.start(MessageInput.FIELD_GLOBAL).in(list).get());

        final DBObject o = findOne(InputImpl.class, new BasicDBObject("$and", query));
        if (o == null) {
            throw new NotFoundException();
        } else {
            return new InputImpl((ObjectId) o.get("_id"), o.toMap());
        }
    }

    @Override
    public void addExtractor(Input input, Extractor extractor) throws ValidationException {
        embed(input, InputImpl.EMBEDDED_EXTRACTORS, extractor);
    }

    @Override
    public void addStaticField(Input input, final String key, final String value) throws ValidationException {
        final EmbeddedPersistable obj = new EmbeddedPersistable() {
            @Override
            public Map<String, Object> getPersistedFields() {
                return ImmutableMap.<String, Object>of(
                        InputImpl.FIELD_STATIC_FIELD_KEY, key,
                        InputImpl.FIELD_STATIC_FIELD_VALUE, value);
            }
        };

        embed(input, InputImpl.EMBEDDED_STATIC_FIELDS, obj);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<Extractor> getExtractors(Input input) {
        if (input.getFields().get(InputImpl.EMBEDDED_EXTRACTORS) == null) {
            return Collections.emptyList();
        }

        final ImmutableList.Builder<Extractor> listBuilder = ImmutableList.builder();
        final BasicDBList mEx = (BasicDBList) input.getFields().get(InputImpl.EMBEDDED_EXTRACTORS);
        for (final Object element : mEx) {
            final DBObject ex = (BasicDBObject) element;

            // SOFT MIGRATION: does this extractor have an order set? Implemented for issue: #726
            Long order = 0l;
            if (ex.containsField(Extractor.FIELD_ORDER)) {
                order = (Long) ex.get(Extractor.FIELD_ORDER); // mongodb driver gives us a java.lang.Long
            }

            try {
                final Extractor extractor = extractorFactory.factory(
                        (String) ex.get(Extractor.FIELD_ID),
                        (String) ex.get(Extractor.FIELD_TITLE),
                        order.intValue(),
                        Extractor.CursorStrategy.valueOf(((String) ex.get(Extractor.FIELD_CURSOR_STRATEGY)).toUpperCase()),
                        Extractor.Type.valueOf(((String) ex.get(Extractor.FIELD_TYPE)).toUpperCase()),
                        (String) ex.get(Extractor.FIELD_SOURCE_FIELD),
                        (String) ex.get(Extractor.FIELD_TARGET_FIELD),
                        (Map<String, Object>) ex.get(Extractor.FIELD_EXTRACTOR_CONFIG),
                        (String) ex.get(Extractor.FIELD_CREATOR_USER_ID),
                        getConvertersOfExtractor(ex),
                        Extractor.ConditionType.valueOf(((String) ex.get(Extractor.FIELD_CONDITION_TYPE)).toUpperCase()),
                        (String) ex.get(Extractor.FIELD_CONDITION_VALUE)
                );

                listBuilder.add(extractor);
            } catch (Exception e) {
                LOG.error("Cannot build extractor from persisted data. Skipping.", e);
            }
        }

        return listBuilder.build();
    }

    @SuppressWarnings("unchecked")
    private List<Converter> getConvertersOfExtractor(DBObject extractor) {
        final ImmutableList.Builder<Converter> listBuilder = ImmutableList.builder();

        final BasicDBList converters = (BasicDBList) extractor.get(Extractor.FIELD_CONVERTERS);
        for (final Object element : converters) {
            final DBObject c = (BasicDBObject) element;

            try {
                listBuilder.add(ConverterFactory.factory(
                        Converter.Type.valueOf(((String) c.get(Extractor.FIELD_CONVERTER_TYPE)).toUpperCase()),
                        (Map<String, Object>) c.get(Extractor.FIELD_CONVERTER_CONFIG)
                ));
            } catch (ConverterFactory.NoSuchConverterException e1) {
                LOG.error("Cannot build converter from persisted data. No such converter.", e1);
            } catch (Exception e) {
                LOG.error("Cannot build converter from persisted data.", e);
            }
        }

        return listBuilder.build();
    }

    @Override
    public void removeExtractor(Input input, String extractorId) {
        removeEmbedded(input, InputImpl.EMBEDDED_EXTRACTORS, extractorId);
    }

    @Override
    public void removeStaticField(Input input, String key) {
        removeEmbedded(input, InputImpl.FIELD_STATIC_FIELD_KEY, InputImpl.EMBEDDED_STATIC_FIELDS, key);
    }

    @Override
    public MessageInput buildMessageInput(Input io) throws NoSuchInputTypeException {
        final Configuration configuration = new Configuration(io.getConfiguration());
        final MessageInput input = messageInputFactory.create(io.getType(), configuration);

        // Add all standard fields.
        input.setTitle(io.getTitle());
        input.setCreatorUserId(io.getCreatorUserId());
        input.setPersistId(io.getId());
        input.setCreatedAt(io.getCreatedAt());
        input.setContentPack(io.getContentPack());

        if (io.isGlobal()) {
            input.setGlobal(true);
        }

        // Add static fields.
        input.addStaticFields(io.getStaticFields());

        input.setConfiguration(configuration);

        return input;
    }

    @Override
    public MessageInput getMessageInput(Input io) throws NoSuchInputTypeException {
        return buildMessageInput(io);
    }
}