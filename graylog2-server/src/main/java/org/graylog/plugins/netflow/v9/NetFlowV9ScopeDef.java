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
package org.graylog.plugins.netflow.v9;

import com.google.auto.value.AutoValue;

@AutoValue
public abstract class NetFlowV9ScopeDef {
    public static final int SYSTEM = 1;
    public static final int INTERFACE = 2;
    public static final int LINECARD = 3;
    public static final int NETFLOW_CACHE = 4;
    public static final int TEMPLATE = 5;

    public abstract int type();

    public abstract int length();

    public static NetFlowV9ScopeDef create(int type, int length) {
        return new AutoValue_NetFlowV9ScopeDef(type, length);
    }
}
