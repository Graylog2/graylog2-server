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
package org.graylog2.database;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.graylog2.plugin.database.Persisted;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

public abstract class PersistedImpl implements Persisted {
    private static final Logger LOG = LoggerFactory.getLogger(PersistedImpl.class);

    protected final Map<String, Object> fields;
    protected final ObjectId id;
    private final AtomicReference<String> hexId = new AtomicReference<>(null);

    protected PersistedImpl(final Map<String, Object> fields) {
        this(new ObjectId(), fields);
    }

    protected PersistedImpl(final ObjectId id, final Map<String, Object> fields) {
        this.id = id;
        this.fields = fields;

        if (null != this.id) {
            hexId.set(this.id.toHexString());
        }

        // Transform all java.util.Date's to JodaTime because MongoDB gives back java.util.Date's. #lol
        for (Map.Entry<String, Object> field : fields.entrySet()) {
            if (field.getValue() instanceof Date) {
                fields.put(field.getKey(), new DateTime(field.getValue(), DateTimeZone.UTC));
            }
        }
    }

    protected ObjectId getObjectId() {
        return this.id;
    }

    @Override
    public String getId() {
        // Performance - toHexString is expensive so we cache it.
        final String s = hexId.get();
        if (s == null && id != null) {
            final String hexString = getObjectId().toHexString();
            hexId.compareAndSet(null, hexString);
            return hexString;
        }

        return s;
    }

    @Override
    @JsonIgnore
    public Map<String, Object> getFields() {
        return fields;
    }

    @Override
    public boolean equals(final Object o) {
        if (!(o instanceof PersistedImpl)) {
            return false;
        }

        final PersistedImpl other = (PersistedImpl) o;
        return Objects.equals(fields, other.fields) && Objects.equals(getObjectId(), other.getObjectId());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getObjectId(), getFields());
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName() + "{" +
                "fields=" + getFields() +
                ", id=" + getId() +
                '}';
    }

    @Override
    public Map<String, Object> asMap() {
        final Map<String, Object> result = new HashMap<>();
        for (Method method : this.getClass().getMethods()) {
            if (method.getName().startsWith("get") && method.getParameterTypes().length == 0) {
                final String fieldName = method.getName().substring(3).toLowerCase();
                try {
                    result.put(fieldName, method.invoke(this));
                } catch (IllegalAccessException | InvocationTargetException e) {
                    LOG.debug("Error while accessing field", e);
                }
            }
        }

        for (Field field : this.getClass().getFields()) {
            if (!result.containsKey(field.getName())) {
                try {
                    result.put(field.getName(), field.get(this));
                } catch (IllegalAccessException e) {
                    LOG.debug("Error while accessing field", e);
                }
            }
        }

        return result;
    }
}
