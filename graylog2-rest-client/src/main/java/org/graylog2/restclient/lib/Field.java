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
package org.graylog2.restclient.lib;

import com.google.common.collect.ImmutableSet;
import com.google.common.hash.Hashing;

import java.nio.charset.StandardCharsets;
import java.util.Set;

public class Field {
    private final String name;
    private final String hash;

    public final static Set<String> STANDARD_SELECTED_FIELDS = ImmutableSet.of(
            "source",
            "message"
    );

    public Field(String field) {
        this.name = field;
        this.hash = Hashing.md5().hashString(field, StandardCharsets.UTF_8).toString();
    }

    public String getName() {
        return name;
    }

    public String getHash() {
        return hash;
    }

    public boolean isStandardSelected() {
        return STANDARD_SELECTED_FIELDS.contains(name);
    }

}
