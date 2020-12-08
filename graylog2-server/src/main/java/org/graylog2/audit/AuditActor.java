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

import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.system.NodeId;

import javax.annotation.Nonnull;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Objects.requireNonNull;

@AutoValue
@WithBeanGetter
public abstract class AuditActor {
    private static final String URN_GRAYLOG_NODE = "urn:graylog:node:";
    private static final String URN_GRAYLOG_USER = "urn:graylog:user:";

    public abstract String urn();

    public static AuditActor user(@Nonnull String username) {
        if (isNullOrEmpty(username)) {
            throw new IllegalArgumentException("username must not be null or empty");
        }
        return new AutoValue_AuditActor(URN_GRAYLOG_USER + username);
    }

    public static AuditActor system(@Nonnull NodeId nodeId) {
        return new AutoValue_AuditActor(URN_GRAYLOG_NODE + requireNonNull(nodeId, "nodeId must not be null").toString());
    }
}
