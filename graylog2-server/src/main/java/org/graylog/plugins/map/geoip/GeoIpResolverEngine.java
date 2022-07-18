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
package org.graylog.plugins.map.geoip;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.net.InetAddresses;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.map.config.GeoIpResolverConfig;
import org.graylog2.plugin.Message;
import org.graylog2.utilities.ReservedIpChecker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.codahale.metrics.MetricRegistry.name;

public class GeoIpResolverEngine {
    private static final Logger LOG = LoggerFactory.getLogger(GeoIpResolverEngine.class);

    /**
     * This is a list of schema fields defined in package <b></b>org.graylog.schema</b> as of 2022-07-15.  If the schema changes,
     * this list must be updated, until a better way of defining schema fields is developed, that allows iteration.
     */
    private static final String[] KNOWN_SCHEMA_IP_FIELDS = {org.graylog.schema.DestinationFields.DESTINATION_IP,
            org.graylog.schema.DestinationFields.DESTINATION_NAT_IP,
            org.graylog.schema.EventFields.EVENT_OBSERVER_IP,
            org.graylog.schema.SourceFields.SOURCE_IP,
            org.graylog.schema.SourceFields.SOURCE_IPV6,
            org.graylog.schema.SourceFields.SOURCE_NAT_IP,
            org.graylog.schema.NetworkFields.NETWORK_FORWARDED_IP,
            org.graylog.schema.AssociatedFields.ASSOCIATED_IP,
            org.graylog.schema.HostFields.HOST_IP,
            org.graylog.schema.HostFields.HOST_IPV6,
            org.graylog.schema.VendorFields.VENDOR_PRIVATE_IP,
            org.graylog.schema.VendorFields.VENDOR_PRIVATE_IPV6,
            org.graylog.schema.VendorFields.VENDOR_PUBLIC_IP,
            org.graylog.schema.VendorFields.VENDOR_PUBLIC_IPV6};

    /**
     * A mapping of fields (per the Graylog Schema) that to search that contain IP addresses.  When the user opts to
     * enforce the Graylog Schema ONLY these fields will be checked; otherwise, all message fields will be checked.
     * to see if they have valid Geo IP information.
     *
     * <p>
     * The mapping is <b>field name</b> -> <b>new message field prefix</b>, where field_name is the field name expected in the message
     * which will be searched in the GeoIP database, and the new message field prefix is the prefix for the new field with the GeoIP data
     * that will be inserted into the message.
     * </p>
     */

    private final Map<String, String> ipAddressFields = Stream.of(KNOWN_SCHEMA_IP_FIELDS)
            .collect(Collectors.toMap(e -> e, mapFieldNameToPrefix()));

    private final GeoIpResolver<GeoLocationInformation> ipLocationResolver;
    private final GeoIpResolver<GeoAsnInformation> ipAsnResolver;
    private final boolean enabled;
    private final boolean enforceGraylogSchema;


    public GeoIpResolverEngine(GeoIpVendorResolverService resolverService, GeoIpResolverConfig config, MetricRegistry metricRegistry) {
        Timer resolveTime = metricRegistry.timer(name(GeoIpResolverEngine.class, "resolveTime"));

        enforceGraylogSchema = config.enforceGraylogSchema();
        ipLocationResolver = resolverService.createCityResolver(config, resolveTime);
        ipAsnResolver = resolverService.createAsnResolver(config, resolveTime);

        LOG.debug("Created Geo IP Resolvers for '{}'", config.databaseVendorType());
        LOG.debug("'{}' Status Enabled: {}", ipLocationResolver.getClass().getSimpleName(), ipLocationResolver.isEnabled());
        LOG.debug("'{}' Status Enabled: {}", ipAsnResolver.getClass().getSimpleName(), ipAsnResolver.isEnabled());

        this.enabled = ipLocationResolver.isEnabled() || ipAsnResolver.isEnabled();

    }

    public boolean filter(Message message) {
        if (!enabled) {
            return false;
        }

        List<String> ipFields = getIpAddressFields(message);

        for (String key : ipFields) {
            Object fieldValue = message.getField(key);
            final InetAddress address = getValidRoutableInetAddress(fieldValue);
            if (address == null) {
                continue;
            }

            // IF the user has opted NOT to enforce the Graylog Schema, the key will likely not
            // be in the field map--in such cases use the key (full field name) as the prefix.
            final String prefix = enforceGraylogSchema ? ipAddressFields.getOrDefault(key, key) : key;

            if (ReservedIpChecker.getInstance().isReservedIpAddress(address.getHostAddress())) {
                message.addField(prefix + "_reserved_ip", true);
            } else {
                addGeoIpDataIfPresent(message, address, prefix);
            }

        }

        return true;
    }

    private void addGeoIpDataIfPresent(Message message, InetAddress address, String newFieldPrefix) {
        ipLocationResolver.getGeoIpData(address).ifPresent(locationInformation -> {
            message.addField(newFieldPrefix + "_geo_coordinates", locationInformation.latitude() + "," + locationInformation.longitude());
            message.addField(newFieldPrefix + "_geo_country_iso", locationInformation.countryIsoCode());
            message.addField(newFieldPrefix + "_geo_city", locationInformation.cityName());
            message.addField(newFieldPrefix + "_geo_region", locationInformation.region());
            message.addField(newFieldPrefix + "_geo_timezone", locationInformation.timeZone());

            if (areValidGeoNames(locationInformation.countryName())) {
                message.addField(newFieldPrefix + "_geo_country", locationInformation.countryName());
            }

            if (areValidGeoNames(locationInformation.cityName(), locationInformation.countryIsoCode())) {
                String name = String.format(Locale.ENGLISH, "%s, %s", locationInformation.cityName(), locationInformation.countryIsoCode());
                message.addField(newFieldPrefix + "_geo_name", name);
            }
        });

        ipAsnResolver.getGeoIpData(address).ifPresent(info -> {

            message.addField(newFieldPrefix + "_as_organization", info.organization());
            message.addField(newFieldPrefix + "_as_number", info.asn());
        });
    }

    /**
     * Get the message fields that will be checked for IP addresses.
     *
     * <p>
     * If the user has chosen NOT to enforce the Graylog Schema, then all fields will be checked as any field could
     * have an IP address.
     * </p>
     *
     * @param message message
     * @return a list of field that may have an IP address
     */
    @VisibleForTesting
    List<String> getIpAddressFields(Message message) {
        return message.getFieldNames()
                .stream()
                .filter(e -> (!enforceGraylogSchema || ipAddressFields.containsKey(e))
                        && !e.startsWith(Message.INTERNAL_FIELD_PREFIX))
                .collect(Collectors.toList());
    }

    private InetAddress getValidRoutableInetAddress(Object fieldValue) {
        final InetAddress ipAddress;
        if (fieldValue instanceof InetAddress) {
            ipAddress = (InetAddress) fieldValue;
        } else if (fieldValue instanceof String) {
            ipAddress = getIpFromFieldValue((String) fieldValue);
        } else {
            ipAddress = null;
        }
        return ipAddress;
    }

    private boolean areValidGeoNames(String... names) {

        for (String name : names) {
            if (StringUtils.isBlank(name) || "N/A".equalsIgnoreCase(name)) {
                return false;
            }
        }

        return true;
    }

    @Nullable
    @VisibleForTesting
    InetAddress getIpFromFieldValue(String fieldValue) {
        try {
            return InetAddresses.forString(fieldValue.trim());
        } catch (IllegalArgumentException e) {
            // Do nothing, field is not an IP
        }

        return null;
    }

    private static Function<String, String> mapFieldNameToPrefix() {
        return string -> string.replace("_ip", "");
    }
}
