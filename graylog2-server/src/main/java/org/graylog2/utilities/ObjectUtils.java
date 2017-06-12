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

public class ObjectUtils {

    private ObjectUtils() {}

    /**
     * Short hand for getting the object identity which is useful to distinguish between instances that override
     * {@link Object#toString()} and/or {@link Object#hashCode()}, especially in log statements.
     *
     * @param object any POJO
     * @return the hex string portion of {@link Object#toString()}
     */
    public static String objectId(Object object) {
        return Integer.toHexString(System.identityHashCode(object));
    }
}
