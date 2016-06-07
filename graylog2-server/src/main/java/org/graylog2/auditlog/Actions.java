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
package org.graylog2.auditlog;

public class Actions {
    private Actions() {
        // Prevent instantiation
    }

    public static final String CREATE = "created";
    public static final String READ = "read";
    public static final String UPDATE = "updated";
    public static final String DELETE = "deleted";
    public static final String START = "started";
    public static final String STOP = "stopped";
    public static final String CANCEL = "cancelled";
    public static final String APPLY = "applied";
    public static final String EXPORT = "exported";
    public static final String SHUTDOWN = "shutdown";
}
