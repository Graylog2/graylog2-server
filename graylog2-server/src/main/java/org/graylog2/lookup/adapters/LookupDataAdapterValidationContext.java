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
package org.graylog2.lookup.adapters;

import com.google.inject.Inject;
import org.graylog2.system.urlwhitelist.UrlWhitelistService;

/**
 * Context object for configurations which require access to services to perform validation.
 */
public class LookupDataAdapterValidationContext {
    private final UrlWhitelistService urlWhitelistService;

    @Inject
    public LookupDataAdapterValidationContext(UrlWhitelistService urlWhitelistService) {
        this.urlWhitelistService = urlWhitelistService;
    }

    public UrlWhitelistService getUrlWhitelistService() {
        return urlWhitelistService;
    }
}
