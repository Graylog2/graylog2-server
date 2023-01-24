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
package org.graylog2.rest.bulk;

import org.graylog.security.HasUser;
import org.graylog2.audit.AuditActor;
import org.graylog2.rest.bulk.model.BulkDeleteRequest;
import org.graylog2.rest.bulk.model.BulkDeleteResponse;

public interface BulkRemover<T, C extends HasUser> {

    BulkDeleteResponse bulkDelete(final BulkDeleteRequest request, final C userContext, final AuditParams params);

    default String getUserName(final C userContext) {
        return userContext.getUser().getName();
    }

    default AuditActor getAuditActor(final C userContext) {
        return AuditActor.user(getUserName(userContext));
    }
}
