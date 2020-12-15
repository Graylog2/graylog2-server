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
package org.graylog2.audit.formatter;

import org.graylog2.audit.AuditActor;

import java.util.Map;

public interface FormattedAuditEvent {
    /**
     * The audit event actor as URN string.
     *
     * Use {@link AuditActor#urn()} to build the URN!
     *
     * Examples:
     *
     *    {@code urn:graylog:user:jane}
     *    {@code urn:graylog:node:28164cbe-4ad9-4c9c-a76e-088655aa7889}
     *
     * @return the actor URN
     */
    String actorUrn();

    /**
     * The audit event namespace.
     *
     * Each plugin should have its own, unique namespace. The Graylog server namespace is {@code server}.
     *
     * @return namespace string
     */
    String namespace();

    /**
     * The audit event object as URN.
     *
     * Examples:
     *
     *   {@code urn:graylog:dashboard:56f2fdefa0275b357744230c:widget:57ab37cc67d0cb54582d43a0}
     *   {@code urn:graylog:message_input:56f2fdefa0275b357744230c}
     *   {@code urn:graylog:pipeline-rule:57ab37cc67d0cb54582d43a0}
     *
     * @return the object URN
     */
    String objectUrn();

    /**
     * The audit event action.
     *
     * A simple string that identifies the action for the object.
     *
     * Examples:
     *
     *   {@code create}
     *   {@code delete}
     *   {@code update}
     *   {@code start}
     *   {@code stop}
     *
     * @return the action
     */
    String action();

    /**
     * The message template string that will be used to present the audit event to humans.
     *
     * All data in {@link #attributes()} as well as the following fields can be used as variables.
     *
     * <ul>
     *     <li>actor</li>
     *     <li>namespace</li>
     *     <li>object</li>
     *     <li>action</li>
     * </ul>
     *
     * Examples:
     *
     *   {@code "Message input ${input_name} created"}
     *
     * @return
     */
    String messageTemplate();

    /**
     * The audit event attributes that will be stored in the database.
     *
     * All information that is needed by the {@link #messageTemplate()} should be in here.
     *
     * Make sure you do not store any sensitive information like passwords and API tokens!
     *
     * @return the audit event attributes
     */
    Map<String, Object> attributes();
}
