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

package org.graylog2.rest.resources.system;

import com.codahale.metrics.Timer;
import com.codahale.metrics.UniformReservoir;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.map.config.GeoIpResolverConfig;
import org.graylog.plugins.map.geoip.GeoAsnInformation;
import org.graylog.plugins.map.geoip.GeoIpResolver;
import org.graylog.plugins.map.geoip.GeoIpVendorResolverService;
import org.graylog.plugins.map.geoip.GeoLocationInformation;
import org.graylog2.plugin.validate.ClusterConfigValidator;
import org.graylog2.plugin.validate.ConfigValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

/**
 * A {@link ClusterConfigValidator} to validate configuration objects of type {@link GeoIpResolverConfig}.
 */
public class GeoIpResolverConfigValidator implements ClusterConfigValidator {

    private static final Logger LOG = LoggerFactory.getLogger(GeoIpResolverConfigValidator.class);

    private final GeoIpVendorResolverService geoIpVendorResolverService;

    @Inject
    public GeoIpResolverConfigValidator(GeoIpVendorResolverService geoIpVendorResolverService) {
        this.geoIpVendorResolverService = geoIpVendorResolverService;
    }

    @Override
    public void validate(Object configObject) throws ConfigValidationException {

        if (configObject instanceof GeoIpResolverConfig) {
            final GeoIpResolverConfig config = (GeoIpResolverConfig) configObject;

            if (config.enabled()) {
                validateConfig(config);
            } else {
                LOG.debug("'{}' is disabled.  Skipping validation", config);
            }
        } else {
            LOG.warn("'{}' cannot be validated with '{}'.  Validator may have been registered incorrectly.", configObject, getClass());
        }
    }

    private void validateConfig(GeoIpResolverConfig config) throws ConfigValidationException {
        Timer timer = new Timer(new UniformReservoir());
        try {
            //A test address.  This will NOT be in any database, but should only produce an
            //AddressNotFoundException.  Any other exception suggests an actual error such as
            //a database file that does does not belong to the vendor selected
            InetAddress testAddress = InetAddress.getByName("127.0.0.1");

            validateGeoIpLocationResolver(config, timer, testAddress);
            validateGeoIpAsnResolver(config, timer, testAddress);

        } catch (UnknownHostException | IllegalArgumentException e) {
            throw new ConfigValidationException(e.getMessage());
        }
    }

    private void validateGeoIpAsnResolver(GeoIpResolverConfig config, Timer timer, InetAddress testAddress) {
        //ASN file is optional--do not validate if not provided.
        if (config.enabled() && StringUtils.isNotBlank(config.asnDbPath())) {
            GeoIpResolver<GeoAsnInformation> asnResolver = geoIpVendorResolverService.createAsnResolver(config, timer);
            if (config.enabled() && !asnResolver.isEnabled()) {
                String msg = String.format(Locale.ENGLISH, "Invalid '%s'  ASN database file '%s'.  Make sure the file exists and is valid for '%1$s'", config.databaseVendorType(), config.asnDbPath());
                throw new IllegalArgumentException(msg);
            }
            asnResolver.getGeoIpData(testAddress);
            if (asnResolver.getLastError().isPresent()) {
                String error = String.format(Locale.ENGLISH, "Error querying ASN.  Make sure you have selected a valid ASN database type for '%s'", config.databaseVendorType());
                throw new IllegalStateException(error);
            }
        }
    }

    private void validateGeoIpLocationResolver(GeoIpResolverConfig config, Timer timer, InetAddress testAddress) {
        GeoIpResolver<GeoLocationInformation> cityResolver = geoIpVendorResolverService.createCityResolver(config, timer);
        if (config.enabled() && !cityResolver.isEnabled()) {
            String msg = String.format(Locale.ENGLISH, "Invalid '%s' City Geo IP database file '%s'.  Make sure the file exists and is valid for '%1$s'", config.databaseVendorType(), config.cityDbPath());
            throw new IllegalArgumentException(msg);
        }


        cityResolver.getGeoIpData(testAddress);
        if (cityResolver.getLastError().isPresent()) {
            String error = String.format(Locale.ENGLISH, "Error querying Geo Location.  Make sure you have selected a valid database type for '%s'", config.databaseVendorType());
            throw new IllegalStateException(error);
        }
    }

}
