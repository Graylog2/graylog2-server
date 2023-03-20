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
package org.graylog.plugins.threatintel.whois.ip;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.whois.WhoisClient;
import org.graylog.plugins.threatintel.whois.ip.parsers.AFRINICResponseParser;
import org.graylog.plugins.threatintel.whois.ip.parsers.APNICResponseParser;
import org.graylog.plugins.threatintel.whois.ip.parsers.ARINResponseParser;
import org.graylog.plugins.threatintel.whois.ip.parsers.LACNICResponseParser;
import org.graylog.plugins.threatintel.whois.ip.parsers.RIPENCCResponseParser;
import org.graylog.plugins.threatintel.whois.ip.parsers.WhoisParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class WhoisIpLookup {

    protected static final Logger LOG = LoggerFactory.getLogger(WhoisIpLookup.class);

    private static final int PORT = 43;

    private final InternetRegistry defaultRegistry;
    private final int connectTimeout;
    private final int readTimeout;
    private final Timer whoisRequestTimer;

    public WhoisIpLookup(WhoisDataAdapter.Config whoisConfig, MetricRegistry metricRegistry) {
        this.defaultRegistry = whoisConfig.registry();
        this.connectTimeout = whoisConfig.connectTimeout();
        this.readTimeout = whoisConfig.readTimeout();

        this.whoisRequestTimer = metricRegistry.timer(MetricRegistry.name(getClass(), "whoisRequestTime"));
    }

    public WhoisIpLookupResult run(String ip) throws Exception {
        return run(this.defaultRegistry, ip);
    }

    public WhoisIpLookupResult run(InternetRegistry registry, String ip) throws Exception {
        // Figure out the right response parser for the registry we are asking.
        WhoisParser parser;
        switch(registry) {
            case AFRINIC:
                parser = new AFRINICResponseParser();
                break;
            case APNIC:
                parser = new APNICResponseParser();
                break;
            case ARIN:
                parser = new ARINResponseParser();
                break;
            case LACNIC:
                parser = new LACNICResponseParser();
                break;
            case RIPENCC:
                parser = new RIPENCCResponseParser();
                break;
            default:
                throw new RuntimeException("No parser implemented for [" + registry.name() + "] responses.");
        }

        final WhoisClient whoisClient = new WhoisClient();
        try {
            whoisClient.setDefaultPort(PORT);
            whoisClient.setConnectTimeout(connectTimeout);
            whoisClient.setDefaultTimeout(readTimeout);
            String query = parser.buildQueryForIp(ip);

            try (final Timer.Context ignored = whoisRequestTimer.time()) {
                whoisClient.connect(registry.getWhoisServer());

                IOUtils.readLines(whoisClient.getInputStream(query), StandardCharsets.UTF_8).forEach(parser::readLine);
            }

            // When we encounter a registry redirect we recursively call this method with the new registry server.
            // We don't want to keep the connection open until all redirects have been processed, so disconnect as
            // soon as we are done reading the response from a server.
            whoisClient.disconnect();

            // Handle registry redirect.
            if(parser.isRedirect()) {
                // STAND BACK FOR STACKOVERFLOWEXCEPTION
                if(registry.equals(parser.getRegistryRedirect())) {
                    /*
                     *                ,--._,--.
                     *              ,'  ,'   ,-`.
                     *   (`-.__    /  ,'   /
                     *    `.   `--'        \__,--'-.
                     *      `--/       ,-.  ______/
                     *        (o-.     ,o- /
                     *         `. ;        \
                     *          |:          \
                     *         ,'`       ,   \
                     *        (o o ,  --'     :
                     *         \--','.        ;
                     *          `;;  :       /
                     *     GARY  ;'  ;  ,' ,'
                     *           ,','  :  '
                     *           \ \   :
                     */
                    LOG.error("{} redirected us back to itself. The Elders of the Internet say: This cannot happen(tm).", registry.toString());
                    return null;
                }

                // Actually run WHOIS request on registry we got redirected to.
                return run(parser.getRegistryRedirect(), ip);
            }

            return new WhoisIpLookupResult(parser.getOrganization(), parser.getCountryCode());
        } catch (IOException e) {
            LOG.error("Could not lookup WHOIS information for [{}] at [{}].", ip, registry.toString());
            throw e;
        } finally {
            if (whoisClient.isConnected()) {
                whoisClient.disconnect();
            }
        }
    }

}
