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

import com.google.common.collect.Iterables;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.plugin.database.validators.Validator;
import org.graylog2.shared.SuppressForbidden;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

@CollectionName(MongoDbSession.COLLECTION_NAME)
public class MongoDbSession extends PersistedImpl {
    private static final Logger LOG = LoggerFactory.getLogger(MongoDbSession.class);
    static final String COLLECTION_NAME = "sessions";
    static final String FIELD_SESSION_ID = "session_id";

    protected MongoDbSession(Map<String, Object> fields) {
        super(fields);
    }

    public MongoDbSession(ObjectId objectId, Map map) {
        super(objectId, map);
    }

    @Override
    public Map<String, Validator> getValidations() {
        return null;
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return null;
    }

    @SuppressForbidden("Deliberate use of ObjectInputStream")
    public Map<Object, Object> getAttributes() {
        final Object attributes = fields.get("attributes");
        if (attributes == null) {
            return null;
        }
        final ByteArrayInputStream bis = new ByteArrayInputStream((byte[]) attributes);
        try {
            // FIXME: This could break backward compatibility if different Java versions are being used.
            final ObjectInputStream ois = new ObjectInputStream(bis);
            final Object o = ois.readObject();
            return (Map<Object, Object>) o;
        } catch (IOException e) {
            LOG.error("little io. wow.", e);
        } catch (ClassNotFoundException e) {
            LOG.error("wrong thingy in db", e);
        }
        return null;
    }

    @SuppressForbidden("Deliberate use of ObjectOutputStream")
    public void setAttributes(Map<Object, Object> attributes) {

        try {
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            // FIXME: This could break backward compatibility if different Java versions are being used.
            final ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(attributes);
            oos.close();
            fields.put("attributes", bos.toByteArray());
        } catch (IOException e) {
            LOG.error("too bad :(", e);
        }
    }

    public Optional<String> getUserIdAttribute() {
        final Map<Object, Object> attributes = getAttributes();
        if (attributes == null) {
            return Optional.empty();
        }

        final Object sessionId;

        // A subject can have more than one principal. If that's the case, the user ID is required to be the first one.
        final Object principals = attributes.get(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
        if (principals instanceof Iterable) {
            sessionId = Iterables.getFirst((Iterable<?>) principals, null);
        } else {
            sessionId = principals;
        }
        return Optional.ofNullable(sessionId).map(String::valueOf);
    }

    public String getHost() {
        return (String) fields.get("host");
    }

    public long getTimeout() {
        final Object timeout = fields.get("timeout");
        if (timeout == null) {
            return 0;
        }
        return ((Number) timeout).longValue();
    }

    public Date getStartTimestamp() {
        return ((DateTime) fields.get("start_timestamp")).toDate();
    }

    public Date getLastAccessTime() {
        return ((DateTime) fields.get("last_access_time")).toDate();
    }

    public void setHost(String host) {
        fields.put("host", host);
    }

    public void setTimeout(long timeout) {
        fields.put("timeout", timeout);
    }

    public void setStartTimestamp(Date startTimestamp) {
        fields.put("start_timestamp", startTimestamp);
    }

    public void setLastAccessTime(Date lastAccessTime) {
        fields.put("last_access_time", lastAccessTime);
    }

    public boolean isExpired() {
        final Object o = fields.get("expired");
        return o == null ? false : (Boolean) o;
    }

    public void setExpired(boolean expired) {
        fields.put("expired", expired);
    }

    public String getSessionId() {
        return String.valueOf(fields.get(FIELD_SESSION_ID));
    }
}
