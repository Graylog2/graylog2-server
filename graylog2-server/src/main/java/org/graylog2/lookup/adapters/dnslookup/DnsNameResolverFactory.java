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
package org.graylog2.lookup.adapters.dnslookup;

import com.google.common.base.Splitter;
import com.google.common.net.HostAndPort;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioDatagramChannel;
import io.netty.resolver.dns.DnsNameResolver;
import io.netty.resolver.dns.DnsNameResolverBuilder;
import io.netty.resolver.dns.DnsServerAddressStreamProvider;
import io.netty.resolver.dns.SequentialDnsServerAddressStreamProvider;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DnsNameResolverFactory {
    private static final Logger LOG = LoggerFactory.getLogger(DnsNameResolverFactory.class);
    private static final int DEFAULT_DNS_PORT = 53;

    private final NioEventLoopGroup eventLoopGroup;
    private String dnsServerIps;
    private final long queryTimeout;

    public DnsNameResolverFactory(NioEventLoopGroup eventLoopGroup, String dnsServerIps, long queryTimeout) {
        this.eventLoopGroup = eventLoopGroup;
        this.dnsServerIps = dnsServerIps;
        this.queryTimeout = queryTimeout;
    }

    public DnsNameResolver create() {
        final List<InetSocketAddress> iNetDnsServerIps = parseServerIpAddresses(dnsServerIps);
        final DnsNameResolverBuilder dnsNameResolverBuilder = new DnsNameResolverBuilder(eventLoopGroup.next());
        dnsNameResolverBuilder.channelType(NioDatagramChannel.class).queryTimeoutMillis(queryTimeout);

        // Specify custom DNS servers if provided. If not, use those specified in local network adapter settings.
        if (CollectionUtils.isNotEmpty(iNetDnsServerIps)) {
            LOG.debug("Attempting to start DNS client with server IPs [{}] on port [{}].",
                    dnsServerIps, DEFAULT_DNS_PORT);

            final DnsServerAddressStreamProvider dnsServer = new SequentialDnsServerAddressStreamProvider(iNetDnsServerIps);
            dnsNameResolverBuilder.nameServerProvider(dnsServer);
        } else {
            LOG.debug("Attempting to start DNS client with custom server IPs [{}] on port [{}].",
                    dnsServerIps, DEFAULT_DNS_PORT);
        }

        return dnsNameResolverBuilder.build();
    }

    private List<InetSocketAddress> parseServerIpAddresses(String dnsServerIps) {

        // Parse and prepare DNS server IP addresses for Netty.
        return StreamSupport
                // Split comma-separated sever IP:port combos.
                .stream(Splitter.on(",").trimResults().omitEmptyStrings().split(dnsServerIps).spliterator(), false)
                // Parse as HostAndPort objects (allows convenient handling of port provided after colon).
                .map(hostAndPort -> HostAndPort.fromString(hostAndPort).withDefaultPort(DEFAULT_DNS_PORT))
                // Convert HostAndPort > InetSocketAddress as required by Netty.
                .map(hostAndPort -> new InetSocketAddress(hostAndPort.getHost(), hostAndPort.getPort()))
                .collect(Collectors.toList());
    }
}
