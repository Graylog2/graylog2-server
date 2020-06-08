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

public class AlertFields {
    public static final String ALERT_CATEGORY = "alert_category";
    public static final String ALERT_DEFINITIONS_VERSION = "alert_definitions_version";
    public static final String ALERT_INDICATOR = "alert_indicator";
    public static final String ALERT_SIGNATURE = "alert_signature";
    public static final String ALERT_SIGNATURE_ID = "alert_signature_id";

    // Derived and Enriched Fields
    public static final String ALERT_SEVERITY = "alert_severity";
    public static final String ALERT_SEVERITY_LEVEL = "alert_severity_level";
}
