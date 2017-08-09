/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.security;

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

    public Optional<String> getUsernameAttribute() {
        final Map<Object, Object> attributes = getAttributes();
        if (attributes == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(String.valueOf(attributes.get(DefaultSubjectContext.PRINCIPALS_SESSION_KEY)));
    }

    public String getHost() {
        return (String) fields.get("host");
    }

    public long getTimeout() {
        final Object timeout = fields.get("timeout");
        if (timeout == null) return 0;
        return (Long) timeout;
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
