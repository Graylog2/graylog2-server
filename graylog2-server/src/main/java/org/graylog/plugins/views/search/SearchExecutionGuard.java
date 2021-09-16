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
package org.graylog.plugins.views.search;

import com.google.common.base.Joiner;
import org.graylog.plugins.views.search.errors.IllegalTimeRangeException;
import org.graylog.plugins.views.search.errors.MissingCapabilitiesException;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;
import org.graylog2.indexer.searches.SearchesClusterConfig;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.indexer.searches.timeranges.TimeRange;
import org.graylog2.plugin.rest.ValidationFailureException;
import org.graylog2.shared.rest.exceptions.MissingStreamPermissionException;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SearchExecutionGuard {

    private static final Logger LOG = LoggerFactory.getLogger(SearchExecutionGuard.class);

    private final Map<String, PluginMetaData> providedCapabilities;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public SearchExecutionGuard(Map<String, PluginMetaData> providedCapabilities, ClusterConfigService clusterConfigService) {
        this.providedCapabilities = providedCapabilities;
        this.clusterConfigService = clusterConfigService;
    }

    public void check(Search search, Predicate<String> hasReadPermissionForStream) {
        checkUserIsPermittedToSeeStreams(search.streamIdsForPermissionsCheck(), hasReadPermissionForStream);
        checkMissingRequirements(search);
        checkTimeRange(search);
    }

    private void checkTimeRange(Search search) {
        final SearchesClusterConfig config = clusterConfigService.get(SearchesClusterConfig.class);

        if (config == null || Period.ZERO.equals(config.queryTimeRangeLimit())) {
            return; // all time ranges allowed, stop checking here
        }

        final Period timeRangeLimit = config.queryTimeRangeLimit();
        final boolean timeRangeLimitViolation = search.queries().stream()
                .map(Query::timerange)
                .filter(Objects::nonNull)
                .filter(tr -> tr.getFrom() != null && tr.getTo() != null)
                .anyMatch(tr -> isOutOfLimit(tr, timeRangeLimit));

        if(timeRangeLimitViolation) {
            throw new IllegalTimeRangeException("Search out of allowed time range limit");
        }
    }

    private boolean isOutOfLimit(TimeRange timeRange, Period limit) {
        final DateTime start = timeRange.getFrom();
        final DateTime end = timeRange.getTo();
        final DateTime allowedStart = end.minus(limit);
        return start.isBefore(allowedStart);
    }

    public void checkUserIsPermittedToSeeStreams(Set<String> streamIds, Predicate<String> hasReadPermissionForStream) {
        final Predicate<String> isForbidden = hasReadPermissionForStream.negate();
        final Set<String> forbiddenStreams = streamIds.stream().filter(isForbidden).collect(Collectors.toSet());

        if (!forbiddenStreams.isEmpty()) {
            throwExceptionMentioningStreamIds(forbiddenStreams);
        }
    }

    private void throwExceptionMentioningStreamIds(Set<String> forbiddenStreams) {
        LOG.warn("Not executing search, it is referencing inaccessible streams: [" + Joiner.on(',').join(forbiddenStreams) + "]");
        throw new MissingStreamPermissionException("The search is referencing at least one stream you are not permitted to see.",
                forbiddenStreams);
    }

    private void checkMissingRequirements(Search search) {
        final Map<String, PluginMetadataSummary> missingRequirements = missingRequirementsForEach(search);
        if (!missingRequirements.isEmpty()) {
            throw new MissingCapabilitiesException(missingRequirements);
        }
    }

    private Map<String, PluginMetadataSummary> missingRequirementsForEach(Search search) {
        return search.requires().entrySet().stream()
                .filter(entry -> !this.providedCapabilities.containsKey(entry.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
