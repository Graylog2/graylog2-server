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
import org.graylog2.rest.bulk.model.BulkOperationFailure;
import org.graylog2.rest.bulk.model.BulkOperationRequest;
import org.graylog2.rest.bulk.model.BulkOperationResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.BadRequestException;

import java.util.LinkedList;
import java.util.List;

/**
 * Implementation of {@link BulkExecutor} that executes bulk operation on entities sequentially, one at a time, using provided {@link SingleEntityOperationExecutor}.
 * It is meant for those type of operations on entities that are very hard to be executed with single MongoDB query in a single request,
 * as many additional checks are to be performed on the fetched entity in order to decide if it is legal to remove it at all.
 *
 * It is unfortunate that it introduces a (N+1) problem, but preparing more complex solution would require extreme changes to current architecture.
 */
public class SequentialBulkExecutor<T, C extends HasUser> implements BulkExecutor<T, C> {

    private static final Logger LOG = LoggerFactory.getLogger(SequentialBulkExecutor.class);

    static final String NO_ENTITY_IDS_ERROR = "No IDs provided in the request";
    private final SingleEntityOperationExecutor<T, C> singleEntityOperationExecutor;
    private final AuditEventSender auditEventSender;
    private final SuccessContextCreator<T> successAuditLogContextCreator;
    private final FailureContextCreator failureAuditLogContextCreator;


    public SequentialBulkExecutor(final SingleEntityOperationExecutor<T, C> singleEntityOperationExecutor,
                                  final AuditEventSender auditEventSender,
                                  final ObjectMapper objectMapper) {
        this.singleEntityOperationExecutor = singleEntityOperationExecutor;
        this.auditEventSender = auditEventSender;
        this.successAuditLogContextCreator = new DefaultSuccessContextCreator<>(new ResponseEntityConverter(objectMapper));
        this.failureAuditLogContextCreator = new DefaultFailureContextCreator();
    }

    public SequentialBulkExecutor(final SingleEntityOperationExecutor<T, C> singleEntityOperationExecutor,
                                  final AuditEventSender auditEventSender,
                                  final SuccessContextCreator<T> successAuditLogContextCreator,
                                  final FailureContextCreator failureAuditLogContextCreator) {
        this.singleEntityOperationExecutor = singleEntityOperationExecutor;
        this.auditEventSender = auditEventSender;
        this.successAuditLogContextCreator = successAuditLogContextCreator;
        this.failureAuditLogContextCreator = failureAuditLogContextCreator;
    }

    /**
     * Executes bulk operation.
     *
     * @param bulkOperationRequest Information about entities that are a subject of execution.
     * @param userContext          Necessary information about a user.
     * @param params               Information needed to create audit log. If null, no audit log will be created.
     * @return {@link BulkOperationResponse} containing the outcome of the bulk operation
     */
    @Override
    public BulkOperationResponse executeBulkOperation(final BulkOperationRequest bulkOperationRequest, final C userContext, final AuditParams params) {
        if (bulkOperationRequest.entityIds() == null || bulkOperationRequest.entityIds().isEmpty()) {
            throw new BadRequestException(NO_ENTITY_IDS_ERROR);
        }

        List<BulkOperationFailure> capturedFailures = new LinkedList<>();
        for (String entityId : bulkOperationRequest.entityIds()) {
            try {
                T entityModel = singleEntityOperationExecutor.execute(entityId, userContext);
                try {
                    if (params != null) {
                        auditEventSender.success(getAuditActor(userContext), params.eventType(), successAuditLogContextCreator.create(entityModel, params.entityClass()));
                    }
                } catch (Exception auditLogStoreException) {
                    //exception on audit log storing should not result in failure report, as the operation itself is successful
                    LOG.error("Failed to store in the audit log information about successful entity removal via bulk action ", auditLogStoreException);
                }
            } catch (Exception ex) {
                capturedFailures.add(new BulkOperationFailure(entityId, ex.getMessage()));
                try {
                    if (params != null) {
                        auditEventSender.failure(getAuditActor(userContext), params.eventType(), failureAuditLogContextCreator.create(params.entityIdInPathParam(), entityId));
                    }
                } catch (Exception auditLogStoreException) {
                    //exception on audit log storing should not result in failure report, as the operation itself is successful
                    LOG.error("Failed to store in the audit log information about failed entity removal via bulk action ", auditLogStoreException);
                }
            }
        }

        return new BulkOperationResponse(
                bulkOperationRequest.entityIds().size() - capturedFailures.size(),
                capturedFailures);
    }
}
