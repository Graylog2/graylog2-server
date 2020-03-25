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
package org.graylog.plugins.views.search;

import com.google.common.base.Joiner;
import org.graylog.plugins.views.search.errors.MissingCapabilitiesException;
import org.graylog.plugins.views.search.views.PluginMetadataSummary;
import org.graylog2.plugin.PluginMetaData;
import org.graylog2.shared.rest.exceptions.MissingStreamPermissionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.ws.rs.ForbiddenException;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SearchExecutionGuard {

    private static final Logger LOG = LoggerFactory.getLogger(SearchExecutionGuard.class);

    private final Map<String, PluginMetaData> providedCapabilities;

    @Inject
    public SearchExecutionGuard(Map<String, PluginMetaData> providedCapabilities) {
        this.providedCapabilities = providedCapabilities;
    }

    public void check(Search search, Predicate<String> hasReadPermissionForStream) {
        checkUserIsPermittedToSeeStreams(search.usedStreamIds(), hasReadPermissionForStream);

        checkMissingRequirements(search);
    }

    private void checkUserIsPermittedToSeeStreams(Set<String> streamIds, Predicate<String> hasReadPermissionForStream) {
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
