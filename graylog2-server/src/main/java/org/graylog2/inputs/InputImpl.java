/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog2.inputs;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBList;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.MapValidator;
import org.graylog2.database.validators.OptionalStringValidator;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.plugin.inputs.MessageInput;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Map;

import static com.google.common.base.Strings.emptyToNull;

@CollectionName("inputs")
public class InputImpl extends PersistedImpl implements Input {
    private static final Logger LOG = LoggerFactory.getLogger(InputImpl.class);

    public static final String FIELD_ID = "_id";
    public static final String FIELD_STATIC_FIELD_KEY = "key";
    public static final String FIELD_STATIC_FIELD_VALUE = "value";

    public static final String EMBEDDED_EXTRACTORS = "extractors";
    public static final String EMBEDDED_STATIC_FIELDS = "static_fields";

    public InputImpl(final Map<String, Object> fields) {
        super(fields);
    }

    public InputImpl(final ObjectId id, final Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public Map<String, Validator> getValidations() {
        final ImmutableMap.Builder<String, Validator> validations = ImmutableMap.builder();
        //validations.put(MessageInput.FIELD_INPUT_ID, new FilledStringValidator());
        validations.put(MessageInput.FIELD_TITLE, new FilledStringValidator());
        validations.put(MessageInput.FIELD_TYPE, new FilledStringValidator());
        validations.put(MessageInput.FIELD_CONFIGURATION, new MapValidator());
        validations.put(MessageInput.FIELD_CREATOR_USER_ID, new FilledStringValidator());
        validations.put(MessageInput.FIELD_CREATED_AT, new DateValidator());
        validations.put(MessageInput.FIELD_CONTENT_PACK, new OptionalStringValidator());

        return validations.build();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        if (EMBEDDED_EXTRACTORS.equals(key)) {
            final ImmutableMap.Builder<String, Validator> validations = ImmutableMap.builder();
            validations.put(Extractor.FIELD_ID, new FilledStringValidator());
            validations.put(Extractor.FIELD_TITLE, new FilledStringValidator());
            validations.put(Extractor.FIELD_TYPE, new FilledStringValidator());
            validations.put(Extractor.FIELD_CURSOR_STRATEGY, new FilledStringValidator());
            validations.put(Extractor.FIELD_TARGET_FIELD, new OptionalStringValidator());
            validations.put(Extractor.FIELD_SOURCE_FIELD, new FilledStringValidator());
            validations.put(Extractor.FIELD_CREATOR_USER_ID, new FilledStringValidator());
            validations.put(Extractor.FIELD_EXTRACTOR_CONFIG, new MapValidator());
            return validations.build();
        }

        if (EMBEDDED_STATIC_FIELDS.equals(key)) {
            return ImmutableMap.of(
                    FIELD_STATIC_FIELD_KEY, new FilledStringValidator(),
                    FIELD_STATIC_FIELD_VALUE, new FilledStringValidator());
        }

        return Collections.emptyMap();
    }

    @Override
    public String getTitle() {
        return (String) fields.get(MessageInput.FIELD_TITLE);
    }

    @Override
    public DateTime getCreatedAt() {
        return new DateTime(fields.get(MessageInput.FIELD_CREATED_AT), DateTimeZone.UTC);
    }

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> getConfiguration() {
        return (Map<String, Object>) fields.get(MessageInput.FIELD_CONFIGURATION);
    }

    @Override
    public Map<String, String> getStaticFields() {
        if (fields.get(EMBEDDED_STATIC_FIELDS) == null) {
            return Collections.emptyMap();
        }

        final BasicDBList list = (BasicDBList) fields.get(EMBEDDED_STATIC_FIELDS);
        final Map<String, String> staticFields = Maps.newHashMapWithExpectedSize(list.size());
        for (final Object element : list) {
            try {
                final DBObject field = (DBObject) element;
                staticFields.put((String) field.get(FIELD_STATIC_FIELD_KEY), (String) field.get(FIELD_STATIC_FIELD_VALUE));
            } catch (Exception e) {
                LOG.error("Cannot build static field from persisted data. Skipping.", e);
            }
        }

        return staticFields;
    }

    @Override
    public String getType() {
        return (String) fields.get(MessageInput.FIELD_TYPE);
    }

    @Override
    public String getCreatorUserId() {
        return (String) fields.get(MessageInput.FIELD_CREATOR_USER_ID);
    }

    @Override
    public Boolean isGlobal() {
        final Object global = fields.get(MessageInput.FIELD_GLOBAL);
        if (global instanceof Boolean) {
            return (Boolean) global;
        } else {
            return false;
        }
    }

    @Override
    public String getContentPack() {
        return (String) fields.get(MessageInput.FIELD_CONTENT_PACK);
    }

    @Override
    public String getNodeId() {
        return emptyToNull((String)fields.get(MessageInput.FIELD_NODE_ID));
    }
}
