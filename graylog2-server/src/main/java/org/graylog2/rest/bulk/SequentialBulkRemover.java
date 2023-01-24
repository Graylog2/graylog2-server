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

import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog.security.HasUser;
import org.graylog2.audit.AuditEventSender;
import org.graylog2.audit.jersey.DefaultFailureContextCreator;
import org.graylog2.audit.jersey.DefaultSuccessContextCreator;
import org.graylog2.audit.jersey.FailureContextCreator;
import org.graylog2.audit.jersey.ResponseEntityConverter;
import org.graylog2.audit.jersey.SuccessContextCreator;
import org.graylog2.rest.bulk.model.BulkDeleteRequest;
import org.graylog2.rest.bulk.model.BulkDeleteResponse;
import org.graylog2.rest.bulk.model.BulkOperationFailure;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of {@link BulkRemover} that removes entities sequentially, one at a time, using provided {@link SingleEntityRemover}.
 * It is meant for those type of entities that are very hard to remove with single MongoDB query in a single request,
 * as many additional checks are to be performed on the fetched entity in order to decide if it is legal to remove it at all...
 */
public class SequentialBulkRemover<T, C extends HasUser> implements BulkRemover<T, C> {

    private static final Logger LOG = LoggerFactory.getLogger(SequentialBulkRemover.class);

    static final BulkOperationFailure NO_ENTITY_IDS_FAILURE = new BulkOperationFailure("", "No IDs provided in the request");
    private final SingleEntityRemover<T, C> singleEntityRemover;
    private final AuditEventSender auditEventSender;
    private final SuccessContextCreator<T> successAuditLogContextCreator;
    private final FailureContextCreator failureAuditLogContextCreator;


    public SequentialBulkRemover(final SingleEntityRemover<T, C> singleEntityRemover,
                                 final AuditEventSender auditEventSender,
                                 final ObjectMapper objectMapper) {
        this.singleEntityRemover = singleEntityRemover;
        this.auditEventSender = auditEventSender;
        this.successAuditLogContextCreator = new DefaultSuccessContextCreator<>(new ResponseEntityConverter(objectMapper));
        this.failureAuditLogContextCreator = new DefaultFailureContextCreator();
    }

    public SequentialBulkRemover(final SingleEntityRemover<T, C> singleEntityRemover,
                                 final AuditEventSender auditEventSender,
                                 final SuccessContextCreator<T> successAuditLogContextCreator,
                                 final FailureContextCreator failureAuditLogContextCreator) {
        this.singleEntityRemover = singleEntityRemover;
        this.auditEventSender = auditEventSender;
        this.successAuditLogContextCreator = successAuditLogContextCreator;
        this.failureAuditLogContextCreator = failureAuditLogContextCreator;
    }

    @Override
    public BulkDeleteResponse bulkDelete(final BulkDeleteRequest bulkDeleteRequest, final C userContext, final AuditParams params) {
        if (bulkDeleteRequest.entityIds() == null || bulkDeleteRequest.entityIds().isEmpty()) {
            return new BulkDeleteResponse(0, List.of(NO_ENTITY_IDS_FAILURE));
        }

        List<BulkOperationFailure> capturedFailures = new LinkedList<>();
        for (String entityId : bulkDeleteRequest.entityIds()) {
            try {
                T entityModel = singleEntityRemover.remove(entityId, userContext);
                try {
                    auditEventSender.success(getAuditActor(userContext), params.eventType(), successAuditLogContextCreator.create(entityModel, params.entityClass()));
                } catch (Exception auditLogStoreException) {
                    //exception on audit log storing should not result in failure report, as the removal itself is successful
                    LOG.error("Failed to store in the audit log information about successful entity removal via bulk action ", auditLogStoreException);
                }
            } catch (Exception ex) {
                capturedFailures.add(new BulkOperationFailure(entityId, ex.getMessage()));
                try {
                    auditEventSender.failure(getAuditActor(userContext), params.eventType(), failureAuditLogContextCreator.create(params.entityIdInPathParam(), entityId));
                } catch (Exception auditLogStoreException) {
                    //exception on audit log storing should not result in failure report, as the removal itself is successful
                    LOG.error("Failed to store in the audit log information about failed entity removal via bulk action ", auditLogStoreException);
                }
            }
        }

        return new BulkDeleteResponse(
                bulkDeleteRequest.entityIds().size() - capturedFailures.size(),
                capturedFailures);
    }
}
