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
package org.graylog2.security;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.DuplicateKeyException;
import org.bson.types.ObjectId;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.PersistedServiceImpl;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.Tools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;

public class AccessTokenServiceImpl extends PersistedServiceImpl implements AccessTokenService {
    private static final Logger LOG = LoggerFactory.getLogger(AccessTokenServiceImpl.class);

    private static final SecureRandom RANDOM = new SecureRandom();

    @Inject
    public AccessTokenServiceImpl(MongoConnection mongoConnection) {
        super(mongoConnection);
    }

    @Override
    @SuppressWarnings("unchecked")
    public AccessToken load(String token) {
        DBObject query = new BasicDBObject();
        query.put(AccessTokenImpl.TOKEN, token);
        final List<DBObject> objects = query(AccessTokenImpl.class, query);

        if (objects.isEmpty()) {
            return null;
        }
        if (objects.size() > 1) {
            LOG.error("Multiple access tokens found, this is a serious bug.");
            throw new IllegalStateException("Access tokens collection has no unique index!");
        }
        final DBObject tokenObject = objects.get(0);
        final Object id = tokenObject.get("_id");
        return new AccessTokenImpl((ObjectId) id, tokenObject.toMap());
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<AccessToken> loadAll(String username) {
        DBObject query = new BasicDBObject();
        query.put(AccessTokenImpl.USERNAME, username);
        final List<DBObject> objects = query(AccessTokenImpl.class, query);
        List<AccessToken> tokens = Lists.newArrayList();
        for (DBObject tokenObject : objects) {
            final Object id = tokenObject.get("_id");
            final AccessToken accessToken = new AccessTokenImpl((ObjectId) id, tokenObject.toMap());
            tokens.add(accessToken);
        }
        return tokens;
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
                id = saveWithoutValidation(accessToken);
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
        accessToken.getFields().put(AccessTokenImpl.LAST_ACCESS, Tools.iso8601());
        save(accessToken);
    }

    @Override
    public String save(AccessToken accessToken) throws ValidationException {
        // make sure we cannot overwrite an existing access token
        collection(AccessTokenImpl.class).createIndex(new BasicDBObject(AccessTokenImpl.TOKEN, 1), new BasicDBObject("unique", true));
        return super.save(accessToken);
    }
}