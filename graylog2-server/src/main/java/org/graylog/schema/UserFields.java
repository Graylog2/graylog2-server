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
package org.graylog.schema;

public enum UserFields {
    USER_COMMAND("user_command"),
    USER_DOMAIN("user_domain"),
    USER_EMAIL("user_email"),
    USER_ID("user_id"),
    USER_NAME("user_name"),

    // Derived and Enriched Fields
    USER_CATEGORY("user_category"),
    USER_PRIORITY("user_priority"),
    USER_PRIORITY_LEVEL("user_priority_level"),
    USER_TYPE("user_type");

    private String value;

    UserFields(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
