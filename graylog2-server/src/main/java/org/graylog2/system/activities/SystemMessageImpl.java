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
package org.graylog2.system.activities;

import com.google.common.collect.ImmutableMap;
import org.bson.types.ObjectId;
import org.graylog2.database.CollectionName;
import org.graylog2.database.PersistedImpl;
import org.graylog2.database.validators.DateValidator;
import org.graylog2.database.validators.FilledStringValidator;
import org.graylog2.plugin.database.validators.Validator;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.util.Collections;
import java.util.Map;

@CollectionName("system_messages")
public class SystemMessageImpl extends PersistedImpl implements SystemMessage {

    private final String caller;
    private final String content;
    private final DateTime timestamp;
    private final String nodeId;

    public SystemMessageImpl(Map<String, Object> fields) {
        super(fields);

        this.caller = fields.get("caller").toString();
        this.content = fields.get("content").toString();
        this.timestamp = new DateTime(fields.get("timestamp").toString(), DateTimeZone.UTC);
        this.nodeId = fields.get("node_id").toString();
    }

    protected SystemMessageImpl(ObjectId id, Map<String, Object> fields) {
        super(id, fields);

        this.caller = (String) fields.get("caller");
        this.content = (String) fields.get("content");
        this.timestamp = new DateTime(fields.get("timestamp"), DateTimeZone.UTC);
        this.nodeId = (String) fields.get("node_id");
    }

    @Override
    public String getCaller() {
        return caller;
    }

    @Override
    public String getContent() {
        return content;
    }

    @Override
    public DateTime getTimestamp() {
        return timestamp;
    }

    @Override
    public String getNodeId() {
        return nodeId;
    }

    @Override
    public Map<String, Validator> getValidations() {
        return ImmutableMap.of(
                "caller", new FilledStringValidator(),
                "content", new FilledStringValidator(),
                "node_id", new FilledStringValidator(),
                "timestamp", new DateValidator());
    }

    @Override
    public Map<String, Validator> getEmbeddedValidations(String key) {
        return Collections.emptyMap();
    }

}