/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.security.shares;

import com.google.auto.value.AutoValue;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableSet;
import org.graylog.grn.GRN;
import org.graylog.grn.GRNDescriptor;
import org.graylog.grn.GRNDescriptorService;
import org.graylog.security.Capability;
import org.graylog.security.DBGrantService;
import org.graylog.security.GrantDTO;
import org.graylog.security.entities.EntityDescriptor;
import org.graylog2.database.PaginatedList;
import org.graylog2.rest.PaginationParameters;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GranteeSharesService {
    private final DBGrantService grantService;
    private final GRNDescriptorService descriptorService;

    @Inject
    public GranteeSharesService(DBGrantService grantService, GRNDescriptorService descriptorService) {
        this.grantService = grantService;
        this.descriptorService = descriptorService;
    }

    public SharesResponse getPaginatedSharesFor(GRN grantee, PaginationParameters paginationParameters) {
        final ImmutableSet<GrantDTO> grants = grantService.getForGrantee(grantee);

        final List<EntityDescriptor> entityDescriptors = grants.stream()
                .map(GrantDTO::target)
                .map(descriptorService::getDescriptor)
                .filter(queryPredicate(paginationParameters))
                .map(grnDescriptor -> EntityDescriptor.create(grnDescriptor.grn(), grnDescriptor.title(), Collections.emptySet()))
                .sorted(Comparator.comparing(EntityDescriptor::title, (t1, t2) -> {
                    if (paginationParameters.getOrder().toLowerCase(Locale.US).equals("desc")) {
                        return t2.compareTo(t1);
                    }
                    return t1.compareTo(t2);
                }))
                .skip(paginationParameters.getPerPage() * (paginationParameters.getPage() - 1))
                .limit(paginationParameters.getPerPage())
                .collect(Collectors.toList());

        final Set<GRN> entityDescriptorsGRNs = entityDescriptors.stream()
                .map(EntityDescriptor::id)
                .collect(Collectors.toSet());

        final Map<GRN, Capability> granteeCapabilities = grants.stream()
                .filter(grant -> entityDescriptorsGRNs.contains(grant.target()))
                .collect(Collectors.toMap(GrantDTO::target, GrantDTO::capability));

        final PaginatedList<EntityDescriptor> paginatedList = new PaginatedList<>(
                entityDescriptors,
                entityDescriptors.size(),
                paginationParameters.getPage(),
                paginationParameters.getPerPage()
        );

        return SharesResponse.create(paginatedList, granteeCapabilities);
    }

    private Predicate<GRNDescriptor> queryPredicate(PaginationParameters paginationParameters) {
        final String query = MoreObjects.firstNonNull(paginationParameters.getQuery(), "").toLowerCase(Locale.US);

        if (query.isEmpty()) {
            return descriptor -> true;
        }

        return descriptor -> descriptor.title().toLowerCase(Locale.US).contains(query);
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
