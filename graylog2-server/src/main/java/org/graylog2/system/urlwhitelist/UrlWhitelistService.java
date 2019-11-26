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

    public UrlWhitelist get() {
        return clusterConfigService.getOrDefault(UrlWhitelist.class, UrlWhitelist.create(Collections.emptyList()));
    }

    public void save(UrlWhitelist whitelist) {
        clusterConfigService.write(whitelist);
    }

    // TODO: add some kind of caching so that we don't fetch from db on every whitelist check
    public boolean isWhitelisted(String url) {
        return get().isWhitelisted(url);
    }

}
