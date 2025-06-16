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
package org.graylog2.inputs.transports.netty;

import com.google.common.base.Splitter;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.net.InetAddresses;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpRequest;
import org.graylog2.utilities.IpSubnet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

public class HttpForwardedForHandler extends SimpleChannelInboundHandler<HttpRequest> {
    private static final Logger LOG = LoggerFactory.getLogger(HttpForwardedForHandler.class);

    // Splits comma-separated forwarded-elements
    private static final Splitter ELEMENT_SPLITTER =
            Splitter.on(',')
                    .omitEmptyStrings()
                    .trimResults();

    // Splits semicolon-separated parameters within an element
    private static final Splitter PARAM_SPLITTER =
            Splitter.on(';')
                    .omitEmptyStrings()
                    .trimResults();

    // Splits key=value pairs
    private static final Splitter KEYVALUE_SPLITTER =
            Splitter.on('=')
                    .limit(2)
                    .trimResults();

    private final boolean enableForwardedFor;
    private final boolean enableRealIpHeader;
    private final List<String> realIpHeaders;
    private final boolean requireTrustedProxies;
    private final Set<IpSubnet> trustedProxies;

    /**
     * @param enableForwardedFor    true if we should parse X-Forwarded-For and Forwarded headers
     * @param realIpHeaders         a comma separated list to check for original IP values, if null or empty don't attempt to find a value
     * @param requireTrustedProxies true if all other values in the chain are checked against our set of trusted proxies
     * @param trustedProxies        config setting from server conf, the subnets of proxies we accept forwarded-for headers from
     */
    public HttpForwardedForHandler(boolean enableForwardedFor, boolean enableRealIpHeader, String realIpHeaders, boolean requireTrustedProxies, Set<IpSubnet> trustedProxies) {
        this.enableForwardedFor = enableForwardedFor;
        this.enableRealIpHeader = enableRealIpHeader;
        this.realIpHeaders = Splitter.on(',').omitEmptyStrings().trimResults().splitToList(Strings.nullToEmpty(realIpHeaders));
        this.requireTrustedProxies = requireTrustedProxies;
        this.trustedProxies = trustedProxies;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, HttpRequest msg) throws Exception {
        String originalIp = null;

        // if we choose to trust X-Forwarded-For or Forwarded headers
        if (enableForwardedFor) {
            // first check for X-Forwarded-For headers
            // the header can be repeated, we need to collect them all and eventually pick the first one
            // the remainder can be checked against our trusted proxies setting
            final List<String> xForwardedForHeaders = msg.headers().getAll("x-forwarded-for");
            final List<String> ipChain = Lists.newArrayList();
            for (String h : xForwardedForHeaders) {
                for (String s : h.split(",")) {
                    String trim = s.trim();
                    if (!trim.isEmpty()) {
                        ipChain.add(trim);
                    }
                }
            }
            if (!ipChain.isEmpty()) {
                final Optional<String> candidate = findOriginalIpAndCheckProxies(ipChain);
                if (candidate.isPresent()) {
                    originalIp = candidate.get();
                    LOG.debug("Found original IP address in X-Forwarded-For header: {}", originalIp);
                }
            }
        }

        // second, check the Forwarded headers
        // https://developer.mozilla.org/en-US/docs/Web/HTTP/Reference/Headers/Forwarded
        // just like the X-Forwarded-For header it can be repeated, both entire headers and the for directives
        // the semantics are the same, the first value represents the original IP
        if (originalIp == null) {
            final List<Map<String, String>> forwardedHeaders = parse(msg.headers());
            // find the first entry that has a for directive, it's our client ip
            // we iterate here because "for" directives are optional as per RFC
            final List<String> forValues = Lists.newArrayList();
            for (Map<String, String> header : forwardedHeaders) {
                if (header.containsKey("for")) {
                    forValues.add(header.get("for"));
                }
            }
            if (!forValues.isEmpty()) {
                final Optional<String> candidate = findOriginalIpAndCheckProxies(forValues);
                if (candidate.isPresent()) {
                    originalIp = candidate.get();
                    LOG.debug("Found original IP address in Forwarded header: {}", originalIp);
                }
            }
        }

        if (originalIp == null && enableRealIpHeader) {
            // iterate over all given headers that are thought to contain a single IP address
            // the list is never null but might be empty
            // the first match wins
            for (final String realIpHeader : realIpHeaders) {
                if (msg.headers().contains(realIpHeader)) {
                    originalIp = msg.headers().get(realIpHeader);
                    LOG.debug("Found original IP address in {} header: {}", realIpHeader, originalIp);
                    break;
                }
            }
        }

        // save the original ip address in the channel's context, which we can pick up in downstream handlers
        if (originalIp != null) {
            if (!InetAddresses.isInetAddress(originalIp)) {
                LOG.warn("Ignoring non-literal IP value for original IP address: {}", originalIp);
            }
            final InetAddress inetAddress = InetAddresses.forString(originalIp);
            ctx.channel().attr(RawMessageHandler.ORIGINAL_IP_KEY).setIfAbsent(new InetSocketAddress(inetAddress, 0));
        }
        ctx.fireChannelRead(msg);
    }

    private Optional<String> findOriginalIpAndCheckProxies(List<String> ipChain) throws UnknownHostException {
        Iterator<String> iterator = ipChain.iterator();
        String candidate = iterator.next();
        if (requireTrustedProxies) {
            LOG.debug("Checking trusted proxies for {}", candidate);
            if (ipChain.isEmpty()) {
                LOG.debug("Ignoring invalid X-Forwarded-For header, list of proxies is empty");
                return Optional.empty();
            } else {
                // go through the remainder of the list, which should all be proxy addresses we trust
                // if we hit one that we don't trust, bail out and don't use the candidate IP
                while (iterator.hasNext()) {
                    final String proxyIp = iterator.next();
                    boolean trusted = false;
                    for (final IpSubnet subnet : trustedProxies) {
                        if (subnet.contains(proxyIp)) {
                            trusted = true;
                            // we can skip the remainder of the subnet checks for this proxy ip
                            break;
                        }
                    }
                    if (!trusted) {
                        LOG.debug("No trusted proxy entry found in {}, ignoring the supplied original IP address {}", trustedProxies, candidate);
                        return Optional.empty();
                    }
                }
            }
        }
        return Optional.of(candidate);
    }

    /**
     * Parses the RFC-7239 Forwarded header into a list of maps,
     * one per forwarded-element (i.e. each comma-separated element).
     *
     * @param headers the msg.headers()
     * @return Map<String, String> per forwarded-element, in order.
     */
    public static List<Map<String, String>> parse(HttpHeaders headers) {
        final List<Map<String, String>> result = Lists.newArrayList();
        // Get _all_ Forwarded header lines, because they can be repeated
        final List<String> raw = headers.getAll("forwarded");

        for (String headerLine : raw) {
            // split into each element: for=..., by=..., proto=...
            for (String element : ELEMENT_SPLITTER.split(headerLine)) {
                final Map<String, String> attrs = PARAM_SPLITTER.splitToList(element)
                        .stream()
                        .map(KEYVALUE_SPLITTER::splitToList)
                        .filter(kv -> kv.size() == 2)
                        .collect(Collectors.toMap(
                                kv -> kv.get(0).toLowerCase(Locale.ROOT), // one of "for", "by", "proto"
                                kv -> stripQuotes(kv.get(1))   // ipv6 values are quoted, so we need to unquote those
                        ));
                result.add(attrs);
            }
        }
        return result;
    }

    /**
     * Helper to remove optional surrounding quotes
     */
    private static String stripQuotes(String s) {
        if (s.length() >= 2 && s.charAt(0) == '"' && s.charAt(s.length() - 1) == '"') {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }
}
