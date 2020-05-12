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
package org.graylog.plugins.views.search.export;

import java.util.Optional;

public class AuditContext {
    private final String userName;
    private final String searchId;
    private final String searchTypeId;

    public AuditContext(String userName, String searchId, String searchTypeId) {
        this.userName = userName;
        this.searchId = searchId;
        this.searchTypeId = searchTypeId;
    }

    public String userName() {
        return userName;
    }

    public Optional<String> searchId() {
        return Optional.ofNullable(searchId);
    }

    public Optional<String> searchTypeId() {
        return Optional.ofNullable(searchTypeId);
    }
}
