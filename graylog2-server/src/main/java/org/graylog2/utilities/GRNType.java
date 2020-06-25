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
package org.graylog2.utilities;

import com.google.auto.value.AutoValue;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Strings.isNullOrEmpty;

@AutoValue
public abstract class GRNType {
    public abstract String type();

    public abstract String permissionPrefix();

    public GRN toGRN(String entity) {
        return newGRNBuilder().entity(entity).build();
    }

    public GRN.Builder newGRNBuilder() {
        return GRN.builder().type(type()).permissionPrefix(permissionPrefix());
    }

    public static GRNType create(String type, String permissionPrefix) {
        checkArgument(!isNullOrEmpty(type), "type cannot be null or empty");
        checkArgument(!isNullOrEmpty(permissionPrefix), "permissionPrefix cannot be null or empty");

        return new AutoValue_GRNType(type, permissionPrefix);
    }
}
