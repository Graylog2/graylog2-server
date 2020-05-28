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

public enum VendorFields {
    VENDOR_ALERT_SEVERITY("vendor_alert_severity"),
    VENDOR_EVENT_ACTION("vendor_event_action"),
    VENDOR_EVENT_DESCRIPTION("vendor_event_description"),
    VENDOR_EVENT_OUTCOME("vendor_event_outcome"),
    VENDOR_EVENT_SECURITY_LEVEL("vendor_event_severity_level"),
    VENDOR_EVENT_SEVERITY("vendor_event_severity"),
    VENDOR_PRIVATE_IP("vendor_private_ip"),
    VENDOR_PRIVATE_IPV6("vendor_private_ipv6"),
    VENDOR_PUBLIC_IP("vendor_public_ip"),
    VENDOR_PUBLIC_IPV6("vendor_public_ipv6"),
    VENDOR_SIGNIN_PROTOCOL("vendor_signin_protocol");

    private String value;

    VendorFields(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
