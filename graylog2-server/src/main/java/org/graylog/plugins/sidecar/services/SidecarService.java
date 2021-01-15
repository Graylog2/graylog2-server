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
package org.graylog.plugins.sidecar.services;

import com.google.common.collect.ImmutableSet;
import com.mongodb.BasicDBObject;
import org.graylog.plugins.sidecar.rest.models.Collector;
import org.graylog.plugins.sidecar.rest.models.CollectorStatus;
import org.graylog.plugins.sidecar.rest.models.CollectorStatusList;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog.plugins.sidecar.rest.models.NodeDetails;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.rest.models.SidecarSummary;
import org.graylog.plugins.sidecar.rest.requests.ConfigurationAssignment;
import org.graylog.plugins.sidecar.rest.requests.RegistrationRequest;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoConnection;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PaginatedDbService;
import org.graylog2.database.PaginatedList;
import org.graylog2.search.SearchQuery;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import org.mongojack.DBQuery;
import org.mongojack.DBSort;

import javax.inject.Inject;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SidecarService extends PaginatedDbService<Sidecar> {
    private static final String COLLECTION_NAME = "sidecars";
    private final CollectorService collectorService;
    private final ConfigurationService configurationService;

    private final Validator validator;

    @Inject
    public SidecarService(CollectorService collectorService,
                          ConfigurationService configurationService,
                          MongoConnection mongoConnection,
                          MongoJackObjectMapperProvider mapper,
                          Validator validator) {
        super(mongoConnection, mapper, Sidecar.class, COLLECTION_NAME);
        this.collectorService = collectorService;
        this.configurationService = configurationService;
        this.validator = validator;

        db.createIndex(new BasicDBObject(Sidecar.FIELD_NODE_ID, 1), new BasicDBObject("unique", true));
    }

    public long count() {
        return db.count();
    }

    @Override
    public Sidecar save(Sidecar sidecar) {
        if (sidecar != null) {
            final Set<ConstraintViolation<Sidecar>> violations = validator.validate(sidecar);
            if (violations.isEmpty()) {
                return db.findAndModify(
                        DBQuery.is(Sidecar.FIELD_NODE_ID, sidecar.nodeId()),
                        new BasicDBObject(),
                        new BasicDBObject(),
                        false,
                        sidecar,
                        true,
                        true);
            } else {
                throw new IllegalArgumentException("Specified object failed validation: " + violations);
            }
        } else
            throw new IllegalArgumentException("Specified object is not of correct implementation type (" + sidecar.getClass() + ")!");
    }

    public List<Sidecar> all() {
        try (final Stream<Sidecar> collectorStream = streamAll()) {
            return collectorStream.collect(Collectors.toList());
        }
    }

    public Sidecar findByNodeId(String id) {
        return db.findOne(DBQuery.is(Sidecar.FIELD_NODE_ID, id));
    }

    public PaginatedList<Sidecar> findPaginated(SearchQuery searchQuery, int page, int perPage, String sortField, String order) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
    }

    public PaginatedList<Sidecar> findPaginated(SearchQuery searchQuery, Predicate<Sidecar> filter, int page, int perPage, String sortField, String order) {
        final DBQuery.Query dbQuery = searchQuery.toDBQuery();
        final DBSort.SortBuilder sortBuilder = getSortBuilder(order, sortField);
        if (filter == null) {
            return findPaginatedWithQueryAndSort(dbQuery, sortBuilder, page, perPage);
        }
        return findPaginatedWithQueryFilterAndSort(dbQuery, filter, sortBuilder, page, perPage);
    }

    public int destroyExpired(Period period) {
        final DateTime threshold = DateTime.now(DateTimeZone.UTC).minus(period);
        int count;

        try (final Stream<Sidecar> collectorStream = streamAll()) {
            count = collectorStream
                    .mapToInt(collector -> {
                        if (collector.lastSeen().isBefore(threshold)) {
                            return delete(collector.id());
                        }
                        return 0;
                    })
                    .sum();
        }

        return count;
    }

    public int markExpired(Period period, String message) {
        final DateTime threshold = DateTime.now(DateTimeZone.UTC).minus(period);
        int count;

        try (final Stream<Sidecar> collectorStream = streamAll()) {
            count = collectorStream
                    .mapToInt(collector -> {
                        if (collector.nodeDetails().statusList() == null) {
                            return 0;
                        }
                        final CollectorStatusList sidecarStatus = collector.nodeDetails().statusList();

                        if (collector.lastSeen().isBefore(threshold) && Sidecar.Status.RUNNING.equals(Sidecar.Status.fromStatusCode(sidecarStatus.status()))) {
                            NodeDetails nodeDetails = collector.nodeDetails();

                            ImmutableSet.Builder<CollectorStatus> collectorStatuses = ImmutableSet.builder();
                            for (CollectorStatus collectorStatus : sidecarStatus.collectors()) {
                                collectorStatuses.add(CollectorStatus.create(
                                        collectorStatus.collectorId(),
                                        Sidecar.Status.UNKNOWN.getStatusCode(),
                                        message, ""));
                            }
                            CollectorStatusList statusListToSave = CollectorStatusList.create(
                                    Sidecar.Status.UNKNOWN.getStatusCode(),
                                    message,
                                    collectorStatuses.build()
                            );
                            NodeDetails nodeDetailsToSave = NodeDetails.create(
                                    nodeDetails.operatingSystem(),
                                    nodeDetails.ip(),
                                    nodeDetails.metrics(),
                                    nodeDetails.logFileList(),
                                    statusListToSave);

                            Sidecar toSave = collector.toBuilder()
                                    .nodeDetails(nodeDetailsToSave)
                                    .build();
                            save(toSave);
                            return 1;

                        }
                        return 0;
                    })
                    .sum();
        }

        return count;
    }

    public Sidecar fromRequest(String nodeId, RegistrationRequest request, String collectorVersion) {
        return Sidecar.create(
                nodeId,
                request.nodeName(),
                NodeDetails.create(
                        request.nodeDetails().operatingSystem(),
                        request.nodeDetails().ip(),
                        request.nodeDetails().metrics(),
                        request.nodeDetails().logFileList(),
                        request.nodeDetails().statusList()),
                collectorVersion);
    }

    public Sidecar assignConfiguration(String collectorNodeId, List<ConfigurationAssignment> assignments) throws NotFoundException{
        Sidecar sidecar = findByNodeId(collectorNodeId);
        if (sidecar == null) {
            throw new NotFoundException("Couldn't find collector with ID " + collectorNodeId);
        }
        for (ConfigurationAssignment assignment : assignments) {
            Collector collector = collectorService.find(assignment.collectorId());
            if (collector == null) {
                throw new NotFoundException("Couldn't find collector with ID " + assignment.collectorId());
            }
            Configuration configuration = configurationService.find(assignment.configurationId());
            if (configuration == null) {
                throw new NotFoundException("Couldn't find configuration with ID " + assignment.configurationId());
            }
            if (!configuration.collectorId().equals(collector.id())) {
                throw new NotFoundException("Configuration doesn't match collector ID " + assignment.collectorId());
            }
        }

        Sidecar toSave = sidecar.toBuilder()
                .assignments(assignments)
                .build();
        return save(toSave);
    }

    public List<SidecarSummary> toSummaryList(List<Sidecar> sidecars, Predicate<Sidecar> isActiveFunction) {
        return sidecars.stream()
                .map(collector -> collector.toSummary(isActiveFunction))
                .collect(Collectors.toList());
    }
}
