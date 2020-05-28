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

public enum NetworkFields {

    NETWORK_APPLICATION("network_application"),
    NETWORK_BYTES("network_application"),
    NETWORK_BYTES_RX("network_bytes_rx"),
    NETWORK_BYTES_TX("network_bytes_tx"),
    NETWORK_COMMUNITY_ID("network_community_id"),
    NETWORK_DIRECTION("network_direction"),
    NETWORK_FORWARDED_IP("network_forwarded_ip"),
    NETWORK_IANA_NUMBER("network_iana_number"),
    NETWORK_INNER("network_inner"),
    NETWORK_INTERFACE_IN("network_interface_oIN"),
    NETWORK_INTERFACE_OUT("network_interface_out"),
    NETWORK_NAME("network_name"),
    NETWORK_PACKETS("network_packets"),
    NETWORK_PROTOCOL("network_protocol"),
    NETWORK_TRANSPORT("network_transport"),
    NETWORK_TUNNEL_DURATION("network_tunnel_duration"),
    NETWORK_TUNNEL_TYPE("network_tunnel_type"),
    NETWORK_TYPE("network_type");

    private String value;

    NetworkFields(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return value;
    }
}
