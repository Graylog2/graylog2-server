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

public enum AlertFields {
    ALERT_CATEGORY("alert_category"),
    ALERT_DEFINITIONS_VERSION("alert_definitions_version"),
    ALERT_SIGNATURE("alert_signature"),
    ALERT_SIGNATURE_CATEGORY("alert_signature_category"),
    ALERT_SIGNATURE_ID("alert_signature_id"),

    // Derived and Enriched Fields
    ALERT_SEVERITY("alert_severity"),
    ALERT_SEVERITY_LEVEL("alert_severity_level");

    private String value;

    AlertFields(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
