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
package org.graylog.security.permissions;

import com.google.auto.value.AutoValue;
import org.apache.shiro.authz.Permission;
import org.graylog.grn.GRN;

@AutoValue
public abstract class GRNPermission implements Permission {
    public abstract String type();

    public abstract GRN target();

    public static GRNPermission create(String type, GRN target) {
        return new AutoValue_GRNPermission(type, target);
    }

    @Override
    public boolean implies(Permission p) {
        // By default only supports comparisons with other GRNPermission
        if (!(p instanceof GRNPermission)) {
            return false;
        }
        GRNPermission other = (GRNPermission) p;

        return (other.type().equals(type()) && other.target().equals(target()));
    }
}
