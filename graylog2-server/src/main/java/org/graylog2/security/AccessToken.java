/*
 * Copyright 2013 TORCH GmbH
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
package org.graylog2.security;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import org.bson.types.ObjectId;
import org.graylog2.Core;
import org.graylog2.database.Persisted;
import org.graylog2.database.ValidationException;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.database.validators.Validator;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

public class AccessToken extends Persisted {
    private static final Logger log = LoggerFactory.getLogger(AccessToken.class);

    public static final String USERNAME = "username";
    public static final String ACCESS_TOKENS = "access_tokens";
    public static final String TOKEN = "token";
    public static final String NAME = "NAME";

    private static final SecureRandom RANDOM = new SecureRandom();
    public static final String LAST_ACCESS = "last_access";

    public AccessToken(Core core, Map<String, Object> fields) {
        super(core, fields);
    }

    public AccessToken(Core core, ObjectId id, Map<String, Object> fields) {
        super(core, id, fields);
    }

    @Override
    public String getCollectionName() {
        return ACCESS_TOKENS;
    }

    @Override
    protected Map<String, Validator> getValidations() {
        Map<String, Validator> validations = Maps.newHashMap();
        validations.put(USERNAME, new FilledStringValidator());
        validations.put(TOKEN, new FilledStringValidator());
        validations.put(NAME, new FilledStringValidator());
        return validations;
    }

    @Override
    protected Map<String, Validator> getEmbeddedValidations(String key) {
        return null;
    }

    @SuppressWarnings("unchecked")
    public static AccessToken load(String token, Core core) {
        DBObject query = new BasicDBObject();
        query.put(TOKEN, token);
        final List<DBObject> objects = query(query, core, ACCESS_TOKENS);

        if (objects.isEmpty()) {
            return null;
        }
        if (objects.size() > 1) {
            log.error("Multiple access tokens found, this is a serious bug.");
            throw new IllegalStateException("Access tokens collection has no unique index!");
        }
        final DBObject tokenObject = objects.get(0);
        final Object id = tokenObject.get("_id");
        return new AccessToken(core, (ObjectId) id, tokenObject.toMap());
    }

    @SuppressWarnings("unchecked")
    public static List<AccessToken> loadAll(String username, Core core) {
        DBObject query = new BasicDBObject();
        query.put(USERNAME, username);
        final List<DBObject> objects = query(query, core, ACCESS_TOKENS);
        List<AccessToken> tokens = Lists.newArrayList();
        for (DBObject tokenObject : objects) {
            final Object id = tokenObject.get("_id");
            final AccessToken accessToken = new AccessToken(core, (ObjectId) id, tokenObject.toMap());
            tokens.add(accessToken);
        }
        return tokens;
    }

    public static AccessToken create(Core core, String username, String name) {
        Map<String, Object> fields = Maps.newHashMap();
        AccessToken accessToken;
        ObjectId id = null;
        int iterations = 0;
        // let's create a unique access token.
        // this loop should never have a collision, but we will try up to 10 times anyway.
        do {
            // 256 bits of randomness should be plenty hard to guess, no?
            final String token = new BigInteger(256, RANDOM).toString(32);
            fields.put(TOKEN, token);
            fields.put(USERNAME, username);
            fields.put(NAME, name);
            fields.put(LAST_ACCESS, Tools.dateTimeFromDouble(0)); // aka never.
            accessToken = new AccessToken(core, fields);
            try {
                id = accessToken.saveWithoutValidation();
            } catch (MongoException.DuplicateKey ignore) {}
        } while (iterations++ < 10 && id == null);
        if (id == null) {
            throw new IllegalStateException("Could not create unique access token, tried 10 times. This is bad.");
        }
        return accessToken;
    }

    public void touch() throws ValidationException {
        fields.put(LAST_ACCESS, DateTime.now(DateTimeZone.UTC));
        save();
    }

    public DateTime getLastAccess() {
        final Object o = fields.get(LAST_ACCESS);
        return (DateTime) o;
    }

    @Override
    public ObjectId save() throws ValidationException {
        // make sure we cannot overwrite an existing access token
        collection().ensureIndex(new BasicDBObject(TOKEN, 1), null, true);
        return super.save();
    }

    public String getUserName() {
        return String.valueOf(fields.get(USERNAME));
    }

    public void setUserName(String userName) {
        fields.put(USERNAME, userName);
    }

    public String getToken() {
        return String.valueOf(fields.get(TOKEN));
    }

    public void setToken(String token) {
        fields.put(TOKEN, token);
    }

    public String getName() {
        return String.valueOf(fields.get(NAME));
    }

    public void setName(String name) {
        fields.put(NAME, name);
    }

}
