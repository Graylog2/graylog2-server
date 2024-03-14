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
package org.graylog.security.certutil.audit;

import com.google.common.collect.ImmutableSet;
import org.graylog2.audit.PluginAuditEventTypes;

import java.util.Set;

public class CaAuditEventTypes implements PluginAuditEventTypes {
    private static final String NAMESPACE = "ca:";

    public static final String CA_CREATE = NAMESPACE + "create";
    public static final String CA_UPLOAD = NAMESPACE + "upload";
    public static final String CLIENTCERT_CREATE = NAMESPACE + "clientcert:create";
    public static final String CLIENTCERT_DELETE = NAMESPACE + "clientcert:delete";

    private static final ImmutableSet<String> EVENT_TYPES = ImmutableSet.<String>builder()
            .add(CA_CREATE)
            .add(CA_UPLOAD)
            .add(CLIENTCERT_CREATE)
            .add(CLIENTCERT_DELETE)
            .build();

    @Override
    public Set<String> auditEventTypes() {
        return EVENT_TYPES;
    }

}
