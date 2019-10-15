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
package org.graylog.plugins.views.search.authorization;

import com.google.common.base.Joiner;
import com.google.common.collect.Sets;
import org.graylog.plugins.views.search.Query;
import org.graylog.plugins.views.search.Search;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;
import org.graylog2.plugin.PluginMetaData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SearchAuthorizer {

    private static final Logger LOG = LoggerFactory.getLogger(SearchAuthorizer.class);

    private final Map<String, PluginMetaData> providedCapabilities;

    @Inject
    public SearchAuthorizer(Map<String, PluginMetaData> providedCapabilities) {
        this.providedCapabilities = providedCapabilities;
    }

    public void authorize(Search search, Predicate<String> hasReadPermissionForStream) {
        checkStreamPermissions(search, hasReadPermissionForStream);

        final Map<String, PluginMetadataSummary> missingRequirements = missingRequirementsForEach(search);
        if (!missingRequirements.isEmpty()) {
            throw new MissingCapabilitiesException(missingRequirements);
        }
    }

    private void checkStreamPermissions(Search search, Predicate<String> hasReadPermissionForStream) {
        final Optional<Set<String>> usedStreamIds = search.queries().stream().map(Query::usedStreamIds).reduce(Sets::union);

        checkUserIsPermittedToSeeStreams(usedStreamIds.orElse(Collections.emptySet()), hasReadPermissionForStream);
    }

    private void checkUserIsPermittedToSeeStreams(Set<String> streamIds, Predicate<String> hasReadPermissionForStream) {
        final Predicate<String> isForbidden = hasReadPermissionForStream.negate();
        final Set<String> forbiddenStreams = streamIds.stream().filter(isForbidden).collect(Collectors.toSet());

        if (!forbiddenStreams.isEmpty()) {
            throwExceptionWithoutMentioningStreamIds(forbiddenStreams);
        }
    }

    private void throwExceptionWithoutMentioningStreamIds(Set<String> forbiddenStreams) {
        LOG.warn("Not executing search, it is referencing inaccessible streams: [" + Joiner.on(',').join(forbiddenStreams) + "]");
        throw new ForbiddenException("The search is referencing at least one stream you are not permitted to see.");
    }

    private Map<String, PluginMetadataSummary> missingRequirementsForEach(Search search) {
        return search.requires().entrySet().stream()
                .filter(entry -> !this.providedCapabilities.containsKey(entry.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
