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
package org.graylog2.system.urlwhitelist;

import com.google.inject.Inject;
import org.graylog2.plugin.cluster.ClusterConfigService;

import java.util.Collections;

public class UrlWhitelistService {

    private final ClusterConfigService clusterConfigService;

    @Inject
    public UrlWhitelistService(ClusterConfigService clusterConfigService) {
        this.clusterConfigService = clusterConfigService;
    }

    /**
     * Gets the whitelist by reading from the cluster configuration.
     *
     * <p>There should always be a whitelist which is created by an initial migration but if there is none, we return a
     * disabled one, which will consider all URLs as whitelisted.</p>
     *
     * <p> This is  because we can't easily guarantee that migrations are run before other services are started. On a
     * system that didn't have a whitelist before, we have to add the URLs configured e.g. in lookup table data adapters
     * to the whitelist by running a migration. If the services start before the migration has run, the configured URLs
     * have to pass the whitelist though, otherwise the services won't be able to run properly. Once the migration has
     * run, these URLs will have been added to whitelist and we are fine.</p>
     */
    public UrlWhitelist get() {
        return clusterConfigService.getOrDefault(UrlWhitelist.class,
                UrlWhitelist.create(Collections.emptyList(), true));
    }

    public void save(UrlWhitelist whitelist) {
        clusterConfigService.write(whitelist);
    }

    // TODO: add some kind of caching so that we don't fetch from db on every whitelist check
    public boolean isWhitelisted(String url) {
        return get().isWhitelisted(url);
    }
}
