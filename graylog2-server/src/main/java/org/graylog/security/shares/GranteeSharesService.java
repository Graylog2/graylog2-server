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
package org.graylog.security.shares;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;
import com.google.common.primitives.Ints;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNDescriptor;
import org.graylog.grn.GRNDescriptorService;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog.security.entities.EntityDescriptor;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.PaginationParameters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.google.common.base.MoreObjects.firstNonNull;

public class GranteeSharesService {
    private static final Logger LOG = LoggerFactory.getLogger(GranteeSharesService.class);

    private final DBGrantService grantService;
    private final GRNDescriptorService descriptorService;
    private final GranteeService granteeService;

    @Inject
    public GranteeSharesService(DBGrantService grantService,
                                GRNDescriptorService descriptorService, GranteeService granteeService) {
        this.grantService = grantService;
        this.descriptorService = descriptorService;
        this.granteeService = granteeService;
    }

    public SharesResponse getPaginatedSharesFor(GRN grantee,
                                                PaginationParameters paginationParameters,
                                                String capabilityFilterString,
                                                String entityTypeFilterString) {
        final Optional<Capability> capability = parseCapabilityFilter(capabilityFilterString);
        // Get all aliases for the grantee to make sure we find all entities the grantee has access to
        final Set<GRN> granteeAliases = granteeService.getGranteeAliases(grantee);
        final ImmutableSet<GrantDTO> grants = capability
                .map(c -> grantService.getForGranteesOrGlobalWithCapability(granteeAliases, c))
                .orElseGet(() -> grantService.getForGranteesOrGlobal(granteeAliases));

        final Set<GRN> targets = grants.stream().map(GrantDTO::target).collect(Collectors.toSet());

        final Map<GRN, Set<EntityDescriptor.Owner>> targetOwners = getTargetOwners(targets);

        final Supplier<Stream<EntityDescriptor>> filteredStream = () -> targets.stream()
                .map(descriptorService::getDescriptor)
                .filter(queryPredicate(paginationParameters))
                .filter(entityTypeFilterPredicate(entityTypeFilterString))
                .map(toEntityDescriptor(targetOwners))
                .sorted(Comparator.comparing(EntityDescriptor::title, (t1, t2) -> {
                    if (paginationParameters.getOrder().toLowerCase(Locale.US).equals("desc")) {
                        return t2.compareTo(t1);
                    }
                    return t1.compareTo(t2);
                }));

        final int filteredResultCount = Ints.saturatedCast(filteredStream.get().count());

        final List<EntityDescriptor> entityDescriptors = filteredStream.get()
                .skip(paginationParameters.getPerPage() * (paginationParameters.getPage() - 1))
                .limit(paginationParameters.getPerPage())
                .collect(Collectors.toList());

        final Set<GRN> entityDescriptorsGRNs = entityDescriptors.stream()
                .map(EntityDescriptor::id)
                .collect(Collectors.toSet());

        final Map<GRN, Capability> granteeCapabilities = grants.stream()
                .filter(grant -> entityDescriptorsGRNs.contains(grant.target()))
                // Group grants by target so we can select the grant with the highest capability priority later
                .collect(Collectors.groupingBy(GrantDTO::target))
                .values()
                .stream()
                // Select the grant with the highest capability priority
                .map(grantsList -> grantsList.stream().max(Comparator.comparing(grant -> grant.capability().priority())))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toMap(GrantDTO::target, GrantDTO::capability));

        final PaginatedList<EntityDescriptor> paginatedList = new PaginatedList<>(
                entityDescriptors,
                filteredResultCount,
                paginationParameters.getPage(),
                paginationParameters.getPerPage(),
                (long) targets.size()
        );

        return SharesResponse.create(paginatedList, granteeCapabilities);
    }

    private Function<GRNDescriptor, EntityDescriptor> toEntityDescriptor(Map<GRN, Set<EntityDescriptor.Owner>> targetOwners) {
        return grnDescriptor -> EntityDescriptor.create(
                grnDescriptor.grn(),
                grnDescriptor.title(),
                targetOwners.getOrDefault(grnDescriptor.grn(), Collections.emptySet())
        );
    }

    private Map<GRN, Set<EntityDescriptor.Owner>> getTargetOwners(Set<GRN> targets) {
        return grantService.getOwnersForTargets(targets).entrySet()
                .stream()
                .map(entry -> Maps.immutableEntry(entry.getKey(), getOwners(entry)))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private Set<EntityDescriptor.Owner> getOwners(Map.Entry<GRN, Set<GRN>> entry) {
        return descriptorService.getDescriptors(entry.getValue())
                .stream()
                .map(descriptor -> EntityDescriptor.Owner.create(descriptor.grn(), descriptor.title()))
                .collect(Collectors.toSet());
    }

    private Optional<Capability> parseCapabilityFilter(String capabilityFilterString) {
        final String capabilityFilter = firstNonNull(capabilityFilterString, "").trim().toUpperCase(Locale.US);

        if (capabilityFilter.isEmpty()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Capability.valueOf(capabilityFilter));
        } catch (IllegalArgumentException e) {
            LOG.warn("Unknown capability", e);
            return Optional.empty();
        }
    }

    private Predicate<GRNDescriptor> queryPredicate(PaginationParameters paginationParameters) {
        final String query = firstNonNull(paginationParameters.getQuery(), "").trim().toLowerCase(Locale.US);

        if (query.isEmpty()) {
            return descriptor -> true;
        }

        return descriptor -> descriptor.title().toLowerCase(Locale.US).contains(query);
    }

    private Predicate<GRNDescriptor> entityTypeFilterPredicate(String entityTypeFilter) {
        final String type = firstNonNull(entityTypeFilter, "").trim().toLowerCase(Locale.US);

        if (type.isEmpty()) {
            return descriptor -> true;
        }

        return descriptor -> descriptor.grn().type().equals(type);
    }

    @AutoValue
    public static abstract class SharesResponse {
        public abstract PaginatedList<EntityDescriptor> paginatedEntities();

        public abstract Map<GRN, Capability> capabilities();

        public static SharesResponse create(PaginatedList<EntityDescriptor> paginatedEntities, Map<GRN, Capability> capabilities) {
            return new AutoValue_GranteeSharesService_SharesResponse(paginatedEntities, capabilities);
        }
    }
}
