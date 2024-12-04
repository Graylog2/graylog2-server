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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.FindOneAndReplaceOptions;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReturnDocument;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.apache.commons.collections4.CollectionUtils;
import org.graylog.plugins.sidecar.rest.models.Collector;
import org.graylog.plugins.sidecar.rest.models.CollectorStatus;
import org.graylog.plugins.sidecar.rest.models.CollectorStatusList;
import org.graylog.plugins.sidecar.rest.models.Configuration;
import org.graylog.plugins.sidecar.rest.models.NodeDetails;
import org.graylog.plugins.sidecar.rest.models.Sidecar;
import org.graylog.plugins.sidecar.rest.models.SidecarSummary;
import org.graylog.plugins.sidecar.rest.requests.ConfigurationAssignment;
import org.graylog.plugins.sidecar.rest.requests.RegistrationRequest;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.pagination.MongoPaginationHelper;
import org.graylog2.database.utils.MongoUtils;
import org.graylog2.notifications.Notification;
import org.graylog2.notifications.NotificationService;
import org.graylog2.notifications.NotificationSystemEventPublisher;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.search.SearchQuery;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.regex;

public class SidecarService {
    private static final String COLLECTION_NAME = "sidecars";
    private final CollectorService collectorService;
    private final ConfigurationService configurationService;
    private final NotificationService notificationService;
    private final NotificationSystemEventPublisher notificationSystemEventPublisher;


    private final Validator validator;
    private final MongoCollection<Sidecar> collection;
    private final MongoPaginationHelper<Sidecar> paginationHelper;
    private final MongoUtils<Sidecar> mongoUtils;

    @Inject
    public SidecarService(CollectorService collectorService,
                          ConfigurationService configurationService,
                          MongoCollections mongoCollections,
                          NotificationService notificationService,
                          NotificationSystemEventPublisher notificationSystemEventPublisher,
                          Validator validator) {
        this.collection = mongoCollections.collection(COLLECTION_NAME, Sidecar.class);
        this.collectorService = collectorService;
        this.configurationService = configurationService;
        this.notificationService = notificationService;
        this.notificationSystemEventPublisher = notificationSystemEventPublisher;
        this.validator = validator;

        collection.createIndex(Indexes.ascending(Sidecar.FIELD_NODE_ID), new IndexOptions().unique(true));
        paginationHelper = mongoCollections.paginationHelper(collection);
        mongoUtils = mongoCollections.utils(collection);
    }

    public long count() {
        return collection.countDocuments();
    }

    public Sidecar save(Sidecar sidecar) {
        Preconditions.checkNotNull(sidecar, "sidecar was null");

        final Set<ConstraintViolation<Sidecar>> violations = validator.validate(sidecar);
        if (!violations.isEmpty()) {
            throw new IllegalArgumentException("Specified object failed validation: " + violations);
        }

        return collection.findOneAndReplace(Filters.eq(Sidecar.FIELD_NODE_ID, sidecar.nodeId()), sidecar,
                new FindOneAndReplaceOptions().returnDocument(ReturnDocument.AFTER).upsert(true));
    }

    // Create new assignments based on tags and existing manual assignments'
    public Sidecar updateTaggedConfigurationAssignments(Sidecar sidecar) {
        final Set<String> sidecarTags = sidecar.nodeDetails().tags();

        // find all configurations that match the tags
        final List<Configuration> taggedConfigs = configurationService.findByTags(sidecarTags);
        final Set<String> matchingOsCollectorIds = collectorService.all().stream()
                .filter(c -> c.nodeOperatingSystem().equalsIgnoreCase(sidecar.nodeDetails().operatingSystem()))
                .map(Collector::id).collect(Collectors.toSet());

        final List<ConfigurationAssignment> tagAssigned = taggedConfigs.stream()
                .filter(c -> matchingOsCollectorIds.contains(c.collectorId())).map(c -> {
                    // fill in ConfigurationAssignment.assignedFromTags()
                    // If we only support one tag on a configuration, this can be simplified
                    final Set<String> matchedTags = c.tags().stream().filter(sidecarTags::contains).collect(Collectors.toSet());
                    return ConfigurationAssignment.create(c.collectorId(), c.id(), matchedTags);
                }).toList();

        final List<ConfigurationAssignment> manuallyAssigned = sidecar.assignments().stream().filter(a -> {
            // also overwrite manually assigned configs that would now be assigned through tags
            if (tagAssigned.stream().anyMatch(tagAssignment -> tagAssignment.configurationId().equals(a.configurationId()))) {
                return false;
            }
            return a.assignedFromTags().isEmpty();
        }).toList();

        // return a sidecar with updated assignments
        final Collection<ConfigurationAssignment> union = CollectionUtils.union(manuallyAssigned, tagAssigned);
        return sidecar.toBuilder().assignments(new ArrayList<>(union)).build();
    }

    public List<Sidecar> all() {
        try (final Stream<Sidecar> collectorStream = streamAll()) {
            return collectorStream.collect(Collectors.toList());
        }
    }

    private Stream<Sidecar> streamAll() {
        return MongoUtils.stream(collection.find());
    }

    public Sidecar findByNodeId(String id) {
        return collection.find(Filters.eq(Sidecar.FIELD_NODE_ID, id)).first();
    }

    public PaginatedList<Sidecar> findPaginated(SearchQuery searchQuery, int page, int perPage, String sortField, SortOrder order) {
        return paginationHelper
                .filter(searchQuery.toBson())
                .sort(order.toBsonSort(sortField))
                .perPage(perPage)
                .page(page);
    }

    public PaginatedList<Sidecar> findPaginated(SearchQuery searchQuery, Predicate<Sidecar> filter, int page, int perPage, String sortField, SortOrder order) {
        final var paginated = paginationHelper
                .filter(searchQuery.toBson())
                .sort(order.toBsonSort(sortField))
                .perPage(perPage);
        if (filter == null) {
            return paginated.page(page);
        }
        return paginated.page(page, filter);
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
                                        message, "", collectorStatus.configurationId()));
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
                                    statusListToSave,
                                    nodeDetails.tags(),
                                    nodeDetails.collectorConfigurationDirectory());

                            Sidecar toSave = collector.toBuilder()
                                    .nodeDetails(nodeDetailsToSave)
                                    .build();
                            save(toSave);

                            createSystemNotification(message, toSave);

                            return 1;

                        }
                        return 0;
                    })
                    .sum();
        }

        return count;
    }

    private void createSystemNotification(String message, Sidecar toSave) {
        Notification notification = notificationService.buildNow();
        notification.addType(Notification.Type.SIDECAR_STATUS_UNKNOWN);
        notification.addSeverity(Notification.Severity.NORMAL);
        notification.addKey(toSave.nodeId());
        notification.addDetail("message", message);
        notification.addDetail("sidecar_name", toSave.nodeName());
        notification.addDetail("sidecar_id", toSave.nodeId());
        notificationSystemEventPublisher.submit(notification);
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
                        request.nodeDetails().statusList(),
                        request.nodeDetails().tags(),
                        request.nodeDetails().collectorConfigurationDirectory()),
                collectorVersion);
    }

    public Sidecar applyManualAssignments(String sidecarNodeId, List<ConfigurationAssignment> assignments) throws NotFoundException {
        Sidecar sidecar = findByNodeId(sidecarNodeId);
        if (sidecar == null) {
            throw new NotFoundException("Couldn't find sidecar with nodeId " + sidecarNodeId);
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

        // Merge manually assigned configurations with tagged ones.
        // This is called from the API. We only allow modifications of untagged assignments.
        final List<ConfigurationAssignment> taggedAssignments = sidecar.assignments().stream().filter(a -> !a.assignedFromTags().isEmpty()).toList();
        final List<String> configIdsAssignedThroughTags = taggedAssignments.stream().map(ConfigurationAssignment::configurationId).toList();

        final List<ConfigurationAssignment> filteredAssignments = assignments.stream().filter(a -> !configIdsAssignedThroughTags.contains(a.configurationId())).toList();
        final Collection<ConfigurationAssignment> union = CollectionUtils.union(filteredAssignments, taggedAssignments);

        Sidecar toSave = sidecar.toBuilder()
                .assignments(new ArrayList<>(union))
                .build();
        return save(toSave);
    }

    public List<SidecarSummary> toSummaryList(List<Sidecar> sidecars, Predicate<Sidecar> isActiveFunction) {
        return sidecars.stream()
                .map(collector -> collector.toSummary(isActiveFunction))
                .collect(Collectors.toList());
    }

    public Stream<Sidecar> findByTagsAndOS(Collection<String> tags, String os) {
        return MongoUtils.stream(collection.find(
                and(
                        in("node_details.tags", tags),
                        regex("node_details.operating_system",
                                Pattern.compile("^" + Pattern.quote(os) + "$", Pattern.CASE_INSENSITIVE))
                ))
        );
    }

    public int delete(String id) {
        return mongoUtils.deleteById(id) ? 1 : 0;
    }
}
