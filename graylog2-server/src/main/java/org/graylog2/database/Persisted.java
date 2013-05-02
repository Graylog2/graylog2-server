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

package org.graylog2.database;

import java.util.List;
import java.util.Map;

import org.bson.types.ObjectId;
import org.graylog2.Core;

import com.beust.jcommander.internal.Lists;
import com.mongodb.BasicDBObject;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.WriteResult;
import org.graylog2.database.validators.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Lennart Koopmann <lennart@torch.sh>
 */
public class Persisted {

    private static final Logger LOG = LoggerFactory.getLogger(Persisted.class);

    protected final Map<String, Object> fields;
    protected final ObjectId id;
    
    protected final Core core;
    protected final String collectionName;
    protected final Map<String, Validator> validations;
    
	protected Persisted(String collectionName, Core core, Map<String, Object> fields, Map<String, Validator> validations) {
        this.id = new ObjectId();
		this.fields = fields;
		
		this.collectionName = collectionName;
		this.core = core;
        this.validations = validations;
	}

	protected Persisted(String collectionName, Core core, ObjectId id, Map<String, Object> fields,  Map<String, Validator> validations) {
		this.id = id;
		this.fields = fields;
		
		this.collectionName = collectionName;
		this.core = core;
        this.validations = validations;
	}
	
	protected static DBObject get(ObjectId id, Core core, String collectionName) {
		return collection(core, collectionName).findOne(new BasicDBObject("_id", id));
	}
	
	protected static List<DBObject> query(DBObject query, Core core, String collectionName) {
		List<DBObject> results = Lists.newArrayList();
		DBCursor cursor = collection(core, collectionName).find(query);

		try {
			while(cursor.hasNext()) {
				results.add(cursor.next());
			}
		} finally {
			cursor.close();
		}
		
		return results;
	}
	
	public void destroy() {
		collection().remove(new BasicDBObject("_id", id));
	}
	
	public ObjectId save() throws ValidationException {
        if(!validate(validations, fields)) {
            throw new ValidationException();
        }

		BasicDBObject doc = new BasicDBObject(fields);
		doc.put("_id", id); // ID was created in constructor or taken from original doc already.

		/*
		 * We are running an upsert. This means that the existing
		 * document will be updated if the ID already exists and
		 * a new document will be created if it doesn't.
		 */
		BasicDBObject q = new BasicDBObject("_id", id);
		collection().update(q, doc, true, false);
		
		return id;
	}
	
	protected DBCollection collection() {
		return collection(this.core, this.collectionName);
	}
	
	protected static DBCollection collection(Core core, String collectionName) {
		return core.getMongoConnection().getDatabase().getCollection(collectionName);
	}

    private boolean validate(Map<String, Validator> validations, Map<String, Object> fields) {
        if (validations == null || validations.isEmpty()) {
            return true;
        }

        for(Map.Entry<String, Validator> validation : validations.entrySet()) {
            Validator v = validation.getValue();
            String field = validation.getKey();

            try {
                if (!v.validate(fields.get(field))) {
                    return false;
                }
            } catch(Exception e) {
                LOG.error("Error while trying to validate <{}>. Marking as invalid.", field, e);
                return false;
            }
        }

        return true;
    }

}
