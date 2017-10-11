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
package org.graylog2.notifications;

import com.google.common.collect.Maps;
import org.bson.types.ObjectId;
import org.graylog2.cluster.Node;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@CollectionName("notifications")
public class NotificationImpl extends PersistedImpl implements Notification {
    static final String FIELD_TYPE = "type";
    static final String FIELD_SEVERITY = "severity";
    static final String FIELD_TIMESTAMP = "timestamp";
    static final String FIELD_NODE_ID = "node_id";
    static final String FIELD_DETAILS = "details";

    private Type type;
    private Severity severity;
    private DateTime timestamp;
    private String node_id;

    protected NotificationImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);

        this.type = Type.valueOf(((String) fields.get(FIELD_TYPE)).toUpperCase(Locale.ENGLISH));
        this.severity = Severity.valueOf(((String) fields.get(FIELD_SEVERITY)).toUpperCase(Locale.ENGLISH));
        this.timestamp = new DateTime(fields.get(FIELD_TIMESTAMP), DateTimeZone.UTC);
        this.node_id = (String) fields.get(FIELD_NODE_ID);
    }

    protected NotificationImpl(Map<String, Object> fields) {
        super(fields);

        this.type = Type.valueOf(((String) fields.get(FIELD_TYPE)).toUpperCase(Locale.ENGLISH));
        this.severity = Severity.valueOf(((String) fields.get(FIELD_SEVERITY)).toUpperCase(Locale.ENGLISH));
        this.timestamp = new DateTime(fields.get(FIELD_TIMESTAMP), DateTimeZone.UTC);
        this.node_id = (String) fields.get(FIELD_NODE_ID);
    }

    public NotificationImpl() {
        super(new HashMap<String, Object>());
    }

    @Override
    public Notification addType(Type type) {
        this.type = type;
        fields.put(FIELD_TYPE, type.toString().toLowerCase(Locale.ENGLISH));
        return this;
    }

    @Override
    public Notification addTimestamp(DateTime timestamp) {
        this.timestamp = timestamp;
        fields.put(FIELD_TIMESTAMP, Tools.getISO8601String(timestamp));

        return this;
    }

    @Override
    public Notification addSeverity(Severity severity) {
        this.severity = severity;
        fields.put(FIELD_SEVERITY, severity.toString().toLowerCase(Locale.ENGLISH));
        return this;
    }

    @Override
    public Notification addNode(Node node) {
        return addNode(node.getNodeId());
    }

    @Override
    public DateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public Type getType() {
        return type;
    }

    @Override
    public Severity getSeverity() {
        return severity;
    }

    @Override
    public String getNodeId() {
        return this.node_id;
    }

    @Override
    public Notification addDetail(String key, Object value) {
        Map<String, Object> details;
        if (fields.get(FIELD_DETAILS) == null)
            fields.put(FIELD_DETAILS, new HashMap<String, Object>());

        details = (Map<String, Object>) fields.get(FIELD_DETAILS);
        details.put(key, value);
        return this;
    }

    @Override
    public Object getDetail(String key) {
        final Map<String, Object> details = (Map<String, Object>) fields.get(FIELD_DETAILS);
        if (details == null)
            return null;

        return details.get(key);
    }

    @Override
    public Map<String, Object> asMap() {
        Map<String, Object> result = Maps.newHashMap(fields);
        result.remove("_id");

        return result;
    }

    @Override
    public Notification addNode(String nodeId) {
        fields.put(FIELD_NODE_ID, nodeId);
        return this;  //To change body of created methods use File | Settings | File Templates.
    }

    @Override
    public Map<String, Validator> getValidations() {
        return Collections.emptyMap();
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Collections.emptyMap();
    }

}
