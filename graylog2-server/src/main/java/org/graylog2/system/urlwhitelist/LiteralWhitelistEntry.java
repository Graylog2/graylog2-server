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

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.annotation.Nullable;

public class LiteralWhitelistEntry extends WhitelistEntry {
    public LiteralWhitelistEntry(@JsonProperty("id") String id, @JsonProperty("value") String value,
            @JsonProperty("title") @Nullable String title) {
        super(id, Type.LITERAL, value, title);
    }

    @Override
    public boolean isWhitelisted(String url) {
        return value().equals(url);
    }
}
