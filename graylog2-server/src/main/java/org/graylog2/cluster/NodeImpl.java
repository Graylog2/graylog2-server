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
package org.graylog2.cluster;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.Map;

@CollectionName("nodes")
public class NodeImpl extends PersistedImpl implements Node {

    protected NodeImpl(Map<String, Object> fields) {
        super(fields);
    }

    protected NodeImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);
    }

    @Override
    public String getNodeId() {
        return (String) fields.get("node_id");
    }

    @Override
    public boolean isMaster() {
        return (Boolean) fields.get("is_master");
    }

    @Override
    public String getTransportAddress() {
        return (String) fields.get("transport_address");
    }

    @Override
    public DateTime getLastSeen() {
        return new DateTime(((Integer) fields.get("last_seen")) * 1000L, DateTimeZone.UTC);
    }

    @Override
    public String getShortNodeId() {
        return getNodeId().split("-")[0];
    }

    @Override
    public Type getType() {
        if (!fields.containsKey("type")) {
            return Type.SERVER;
        }

        return Type.valueOf(fields.get("type").toString().toUpperCase());
    }

    @Override
    @JsonIgnore
    public Map<String, Validator> getValidations() {
        return Collections.emptyMap();
    }

    @Override
    @JsonIgnore
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Collections.emptyMap();
    }

}
