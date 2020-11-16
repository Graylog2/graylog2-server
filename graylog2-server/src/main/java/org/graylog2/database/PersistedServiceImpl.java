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
package org.graylog2.database;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import org.bson.types.ObjectId;
import org.graylog2.plugin.database.EmbeddedPersistable;
import org.graylog2.plugin.database.Persisted;
import org.graylog2.plugin.database.PersistedService;
import org.graylog2.plugin.database.ValidationException;
import org.graylog2.plugin.database.validators.ValidationResult;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.plugin.system.NodeId;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PersistedServiceImpl implements PersistedService {
    private static final Logger LOG = LoggerFactory.getLogger(PersistedServiceImpl.class);
    public final MongoConnection mongoConnection;

    protected PersistedServiceImpl(MongoConnection mongoConnection) {
        this.mongoConnection = mongoConnection;
    }

    protected DBObject get(ObjectId id, String collectionName) {
        return collection(collectionName).findOne(new BasicDBObject("_id", id));
    }

    protected <T extends Persisted> DBObject get(Class<T> modelClass, ObjectId id) {
        return collection(modelClass).findOne(new BasicDBObject("_id", id));
    }

    protected <T extends Persisted> DBObject get(Class<T> modelClass, String id) {
        return get(modelClass, new ObjectId(id));
    }

    protected List<DBObject> query(DBObject query, String collectionName) {
        return query(query, collection(collectionName));
    }

    protected List<DBObject> query(DBObject query, DBCollection collection) {
        return cursorToList(collection.find(query));
    }

    protected <T extends Persisted> List<DBObject> query(Class<T> modelClass, DBObject query) {
        return query(query, collection(modelClass));
    }

    protected <T extends Persisted> List<DBObject> query(Class<T> modelClass, DBObject query, DBObject sort) {
        return cursorToList(collection(modelClass).find(query).sort(sort));
    }

    protected <T extends Persisted> List<DBObject> query(Class<T> modelClass, DBObject query, DBObject sort, int limit, int offset) {
        return cursorToList(
                collection(modelClass)
                        .find(query)
                        .sort(sort)
                        .limit(limit)
                        .skip(offset)
        );
    }

    protected long count(DBObject query, String collectionName) {
        return collection(collectionName).count(query);
    }

    protected <T extends Persisted> long count(Class<T> modelClass, DBObject query) {
        return collection(modelClass).count(query);
    }

    private DBCollection collection(String collectionName) {
        return mongoConnection.getDatabase().getCollection(collectionName);
    }

    protected <T extends Persisted> DBCollection collection(Class<T> modelClass) {
        CollectionName collectionNameAnnotation = modelClass.getAnnotation(CollectionName.class);
        if (collectionNameAnnotation == null) {
            throw new RuntimeException("Unable to determine collection for class " + modelClass.getCanonicalName());
        }
        final String collectionName = collectionNameAnnotation.value();

        return collection(collectionName);
    }

    protected <T extends Persisted> DBCollection collection(T model) {
        return collection(model.getClass());
    }

    protected List<DBObject> cursorToList(DBCursor cursor) {
        if (cursor == null) {
            return Collections.emptyList();
        }

        try {
            return Lists.newArrayList((Iterable<DBObject>) cursor);
        } finally {
            cursor.close();
        }
    }

    protected <T extends Persisted> DBObject findOne(Class<T> model, DBObject query) {
        return collection(model).findOne(query);
    }

    protected <T extends Persisted> DBObject findOne(Class<T> model, DBObject query, DBObject sort) {
        return collection(model).findOne(query, new BasicDBObject(), sort);
    }

    protected DBObject findOne(DBObject query, String collectionName) {
        return collection(collectionName).findOne(query);
    }

    protected DBObject findOne(DBObject query, DBObject sort, String collectioName) {
        return collection(collectioName).findOne(query, new BasicDBObject(), sort);
    }

    protected long totalCount(String collectionName) {
        return collection(collectionName).count();
    }

    protected <T extends Persisted> long totalCount(Class<T> modelClass) {
        return collection(modelClass).count();
    }

    @Override
    public <T extends Persisted> int destroy(T model) {
        return collection(model).remove(new BasicDBObject("_id", new ObjectId(model.getId()))).getN();
    }

    @Override
    public <T extends Persisted> int destroyAll(Class<T> modelClass) {
        return collection(modelClass).remove(new BasicDBObject()).getN();
    }

    protected int destroyAll(String collectionName) {
        return collection(collectionName).remove(new BasicDBObject()).getN();
    }

    protected int destroy(DBObject query, String collectionName) {
        return collection(collectionName).remove(query).getN();
    }

    protected <T extends Persisted> int destroyAll(Class<T> modelClass, DBObject query) {
        return collection(modelClass).remove(query).getN();
    }

    @Override
    public <T extends Persisted> String save(T model) throws ValidationException {
        Map<String, List<ValidationResult>> errors = validate(model, model.getFields());
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        BasicDBObject doc = new BasicDBObject(model.getFields());
        doc.put("_id", new ObjectId(model.getId())); // ID was created in constructor or taken from original doc already.

        // Do field transformations
        fieldTransformations(doc);

		/*
         * We are running an upsert. This means that the existing
		 * document will be updated if the ID already exists and
		 * a new document will be created if it doesn't.
		 */
        BasicDBObject q = new BasicDBObject("_id", new ObjectId(model.getId()));
        collection(model).update(q, doc, true, false);

        return model.getId();
    }

    @Override
    public <T extends Persisted> String saveWithoutValidation(T model) {
        try {
            return save(model);
        } catch (ValidationException ignored) {
            return null;
        }
    }

    @Override
    public <T extends Persisted> Map<String, List<ValidationResult>> validate(T model, Map<String, Object> fields) {
        return validate(model.getValidations(), fields);
    }

    @Override
    public Map<String, List<ValidationResult>> validate(Map<String, Validator> validators, Map<String, Object> fields) {
        if (validators == null || validators.isEmpty()) {
            return Collections.emptyMap();
        }

        final Map<String, List<ValidationResult>> validationErrors = new HashMap<>();
        for (Map.Entry<String, Validator> validation : validators.entrySet()) {
            Validator v = validation.getValue();
            String field = validation.getKey();

            try {
                ValidationResult validationResult = v.validate(fields.get(field));
                if (validationResult instanceof ValidationResult.ValidationFailed) {
                    LOG.debug("Validation failure: [{}] on field [{}]", v.getClass().getCanonicalName(), field);
                    validationErrors.computeIfAbsent(field, k -> new ArrayList<>());
                    validationErrors.get(field).add(validationResult);
                }
            } catch (Exception e) {
                final String error = "Error while trying to validate <" + field + ">, got exception: " + e;
                LOG.debug(error);
                validationErrors.computeIfAbsent(field, k -> new ArrayList<>());
                validationErrors.get(field).add(new ValidationResult.ValidationFailed(error));
            }
        }

        return validationErrors;
    }

    @Override
    public <T extends Persisted> Map<String, List<ValidationResult>> validate(T model) {
        return validate(model, model.getFields());
    }

    protected <T extends Persisted> void embed(T model, String key, EmbeddedPersistable o) throws ValidationException {
        Map<String, List<ValidationResult>> errors = validate(model.getEmbeddedValidations(key), o.getPersistedFields());
        if (!errors.isEmpty()) {
            throw new ValidationException(errors);
        }

        Map<String, Object> fields = Maps.newHashMap(o.getPersistedFields());
        fieldTransformations(fields);

        BasicDBObject dbo = new BasicDBObject(fields);
        collection(model).update(new BasicDBObject("_id", new ObjectId(model.getId())), new BasicDBObject("$push", new BasicDBObject(key, dbo)));
    }

    protected <T extends Persisted> void removeEmbedded(T model, String key, String searchId) {
        BasicDBObject aryQry = new BasicDBObject("id", searchId);
        BasicDBObject qry = new BasicDBObject("_id", new ObjectId(model.getId()));
        BasicDBObject update = new BasicDBObject("$pull", new BasicDBObject(key, aryQry));

        // http://docs.mongodb.org/manual/reference/operator/pull/

        collection(model).update(qry, update);
    }

    protected <T extends Persisted> void removeEmbedded(T model, String arrayKey, String key, String searchId) {
        BasicDBObject aryQry = new BasicDBObject(arrayKey, searchId);
        BasicDBObject qry = new BasicDBObject("_id", new ObjectId(model.getId()));
        BasicDBObject update = new BasicDBObject("$pull", new BasicDBObject(key, aryQry));

        // http://docs.mongodb.org/manual/reference/operator/pull/

        collection(model).update(qry, update);
    }

    private void fieldTransformations(Map<String, Object> doc) {
        for (Map.Entry<String, Object> x : doc.entrySet()) {

            // Work on embedded Maps, too.
            if (x.getValue() instanceof Map) {
                x.setValue(Maps.newHashMap((Map<String, Object>) x.getValue()));
                fieldTransformations((Map<String, Object>) x.getValue());
                continue;
            }

            // JodaTime DateTime is not accepted by MongoDB. Convert to java.util.Date...
            if (x.getValue() instanceof DateTime) {
                doc.put(x.getKey(), ((DateTime) x.getValue()).toDate());
            }

            // Our own NodeID
            if (x.getValue() instanceof NodeId) {
                doc.put(x.getKey(), x.getValue().toString());
            }

        }
    }

}