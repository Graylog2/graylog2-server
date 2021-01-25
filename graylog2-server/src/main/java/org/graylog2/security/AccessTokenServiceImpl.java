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
package org.graylog2.security;

import com.google.common.collect.Maps;
import com.mongodb.BasicDBObject;
import com.mongodb.BasicDBObjectBuilder;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import org.apache.commons.lang3.StringUtils;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import javax.inject.Inject;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Provides access to access tokens in the database.
 * <p/>
 * The token value will automatically be encrypted/decrypted when storing/loading the token object from the database.
 * That means the token value is encrypted at rest but the loaded {@link AccessToken} always contains the plain text value.
 */
public class AccessTokenServiceImpl extends PersistedServiceImpl implements AccessTokenService {
    private static final Logger LOG = LoggerFactory.getLogger(AccessTokenServiceImpl.class);

    private static final SecureRandom RANDOM = new SecureRandom();
    private final AccessTokenCipher cipher;

    @Inject
    public AccessTokenServiceImpl(MongoConnection mongoConnection, AccessTokenCipher accessTokenCipher) {
        super(mongoConnection);
        this.cipher = accessTokenCipher;
        collection(AccessTokenImpl.class).createIndex(new BasicDBObject(AccessTokenImpl.TOKEN_TYPE, 1));
    }

    @Override
    @SuppressWarnings("unchecked")
    public AccessToken load(String token) {
        DBObject query = new BasicDBObject();
        query.put(AccessTokenImpl.TOKEN, cipher.encrypt(token));
        final List<DBObject> objects = query(AccessTokenImpl.class, query);

        if (objects.isEmpty()) {
            return null;
        }
        if (objects.size() > 1) {
            LOG.error("Multiple access tokens found, this is a serious bug.");
            throw new IllegalStateException("Access tokens collection has no unique index!");
        }
        return fromDBObject(objects.get(0));
    }

    @Nullable
    @Override
    public AccessToken loadById(String id) {
        try {
            final DBObject dbObject = get(AccessTokenImpl.class, id);
            if (dbObject != null) {
                return fromDBObject(dbObject);
            }
        } catch (IllegalArgumentException e) {
            // Happens when id is not a valid BSON ObjectId
            LOG.debug("Couldn't load access token", e);
        }
        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AccessToken> loadAll(String username) {
        DBObject query = new BasicDBObject();
        query.put(AccessTokenImpl.USERNAME, username);
        final List<DBObject> objects = query(AccessTokenImpl.class, query);
        return objects.stream()
                .map(this::fromDBObject)
                .collect(Collectors.toList());
    }

    @Override
    public AccessToken create(String username, String name) {
        Map<String, Object> fields = Maps.newHashMap();
        AccessTokenImpl accessToken;
        String id = null;
        int iterations = 0;
        // let's create a unique access token.
        // this loop should never have a collision, but we will try up to 10 times anyway.
        do {
            // 256 bits of randomness should be plenty hard to guess, no?
            final String token = new BigInteger(256, RANDOM).toString(32);
            fields.put(AccessTokenImpl.TOKEN, token);
            fields.put(AccessTokenImpl.USERNAME, username);
            fields.put(AccessTokenImpl.NAME, name);
            fields.put(AccessTokenImpl.LAST_ACCESS, Tools.dateTimeFromDouble(0)); // aka never.
            accessToken = new AccessTokenImpl(fields);
            try {
                id = saveWithoutValidation(encrypt(accessToken));
            } catch (DuplicateKeyException ignore) {
            }
        } while (iterations++ < 10 && id == null);
        if (id == null) {
            throw new IllegalStateException("Could not create unique access token, tried 10 times. This is bad.");
        }
        return accessToken;
    }

    @Override
    public void touch(AccessToken accessToken) throws ValidationException {
        accessToken.getFields().put(AccessTokenImpl.LAST_ACCESS, Tools.nowUTC());
        save(accessToken);
    }

    @Override
    public String save(AccessToken accessToken) throws ValidationException {
        // make sure we cannot overwrite an existing access token
        collection(AccessTokenImpl.class).createIndex(new BasicDBObject(AccessTokenImpl.TOKEN, 1), new BasicDBObject("unique", true));
        return super.save(encrypt(accessToken));
    }

    @Override
    public int deleteAllForUser(String username) {
        LOG.debug("Deleting all access tokens of user \"{}\"", username);
        final DBObject query = BasicDBObjectBuilder.start(AccessTokenImpl.USERNAME, username).get();
        final int result = destroy(query, AccessTokenImpl.COLLECTION_NAME);
        LOG.debug("Deleted {} access tokens of user \"{}\"", result, username);
        return result;
    }

    @SuppressWarnings("unchecked")
    private AccessTokenImpl fromDBObject(DBObject dbObject) {
        final Map<String, Object> fields = new HashMap<String, Object>(dbObject.toMap());
        final String ciphertext = (String) fields.get(AccessTokenImpl.TOKEN);
        if (StringUtils.isNotBlank(ciphertext)) {
            fields.put(AccessTokenImpl.TOKEN, cipher.decrypt(ciphertext));
            // The token type field is only used internally for now so we don't want to expose it
            fields.remove(AccessTokenImpl.TOKEN_TYPE);
        }
        final ObjectId id = (ObjectId) dbObject.get("_id");
        return new AccessTokenImpl(id, fields);
    }

    private AccessTokenImpl encrypt(AccessToken token) {
        Map<String, Object> fields = new HashMap<>(token.getFields());
        final String cleartext = (String) fields.get(AccessTokenImpl.TOKEN);
        if (StringUtils.isNotBlank(cleartext)) {
            fields.put(AccessTokenImpl.TOKEN, cipher.encrypt(cleartext));
            // The token type is used to state the algorithm that is used to encrypt the value
            fields.put(AccessTokenImpl.TOKEN_TYPE, AccessTokenImpl.Type.AES_SIV.getIntValue());
        }
        if (token.getId() == null) {
            return new AccessTokenImpl(fields);
        } else {
            return new AccessTokenImpl(new ObjectId(token.getId()), fields);
        }
    }
}
