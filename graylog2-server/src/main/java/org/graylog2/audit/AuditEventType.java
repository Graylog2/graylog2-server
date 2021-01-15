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
package org.graylog2.audit;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nonnull;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

/**
 * Represents an audit event with namespace, object and action.
 *
 * Plugins should use their own namespace for audit events.
 *
 * The {@link #create(String)} method expects an event type string with the following format:
 *
 *     {@code namespace:object:action}
 *
 * Examples:
 *
 *     {@code server:message_input:create}
 *     {@code pipeline-processor:configuration:update}
 */
@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class AuditEventType {
    private static final String FIELD_NAMESPACE = "namespace";
    private static final String FIELD_OBJECT = "object";
    private static final String FIELD_ACTION = "action";
    private static final String ARGUMENT_ERROR = "Type string needs to be in the following format: <namespace>:<object>:<action> - given string: ";
    private static final Splitter SPLITTER = Splitter.on(":").limit(3);
    private static final Joiner JOINER = Joiner.on(":");

    @JsonProperty(FIELD_NAMESPACE)
    public abstract String namespace();

    @JsonProperty(FIELD_OBJECT)
    public abstract String object();

    @JsonProperty(FIELD_ACTION)
    public abstract String action();

    public String toTypeString() {
        return JOINER.join(namespace(), object(), action());
    }

    @JsonCreator
    public static AuditEventType create(@JsonProperty(FIELD_NAMESPACE) String namespace,
                                        @JsonProperty(FIELD_OBJECT) String object,
                                        @JsonProperty(FIELD_ACTION) String action) {
        return new AutoValue_AuditEventType(namespace, object, action);
    }

    /**
     * Creates {@link AuditEventType} from an audit event type string with the following format.
     *
     *     {@code namespace:object:action}
     *
     * See class documentation for details.
     *
     * @param type the audit event type string
     * @return the object
     */
    public static AuditEventType create(@Nonnull String type) {
        if (isNullOrEmpty(type)) {
            throw new IllegalArgumentException(ARGUMENT_ERROR + type);
        }

        final List<String> strings = SPLITTER.splitToList(type);

        if (strings.size() < 3) {
            throw new IllegalArgumentException(ARGUMENT_ERROR + type);
        }

        final String namespace = strings.get(0);
        final String object = strings.get(1);
        final String action = strings.get(2);

        if (isNullOrEmpty(namespace) || isNullOrEmpty(object) || isNullOrEmpty(action)) {
            throw new IllegalArgumentException(ARGUMENT_ERROR + type);
        }
        return create(namespace, object, action);
    }
}