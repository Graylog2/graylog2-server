/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.schema;

public class NetworkFields {
    public static final String NETWORK_APPLICATION = "network_application";
    public static final String NETWORK_BYTES = "network_bytes";
    public static final String NETWORK_COMMUNITY_ID = "network_community_id";
    public static final String NETWORK_DATA_BYTES = "network_data_bytes";
    public static final String NETWORK_DIRECTION = "network_direction";
    public static final String NETWORK_FORWARDED_IP = "network_forwarded_ip";
    public static final String NETWORK_HEADER_BYTES = "network_header_bytes";
    public static final String NETWORK_IANA_NUMBER = "network_iana_number";
    public static final String NETWORK_ICMP_TYPE = "network_icmp_type";
    public static final String NETWORK_INNER = "network_inner";
    public static final String NETWORK_INTERFACE_IN = "network_interface_in";
    public static final String NETWORK_INTERFACE_OUT = "network_interface_out";
    public static final String NETWORK_IP_VERSION = "network_ip_version";
    public static final String NETWORK_NAME = "network_name";
    public static final String NETWORK_PACKETS = "network_packets";
    public static final String NETWORK_PROTOCOL = "network_protocol";
    public static final String NETWORK_TRANSPORT = "network_transport";
    public static final String NETWORK_TUNNEL_DURATION = "network_tunnel_duration";
    public static final String NETWORK_TUNNEL_TYPE = "network_tunnel_type";
    public static final String NETWORK_TYPE = "network_type";

    // To be removed
    @Deprecated
    public static final String NETWORK_BYTES_RX = DestinationFields.DESTINATION_BYTES_SENT;
    @Deprecated
    public static final String NETWORK_BYTES_TX = SourceFields.SOURCE_BYTES_SENT;
}
