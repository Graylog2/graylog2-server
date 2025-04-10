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
import jakarta.inject.Inject;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.map.config.CloudDownloadException;
import org.graylog.plugins.map.config.GeoIpFileService;
import org.graylog.plugins.map.config.GeoIpFileServiceFactory;
import org.graylog.plugins.map.config.GeoIpResolverConfig;
import org.graylog.plugins.map.geoip.GeoAsnInformation;
import org.graylog.plugins.map.geoip.GeoIpResolver;
import org.graylog.plugins.map.geoip.GeoIpVendorResolverService;
import org.graylog.plugins.map.geoip.GeoLocationInformation;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.validate.ClusterConfigValidator;
import org.graylog2.plugin.validate.ConfigValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * A {@link ClusterConfigValidator} to validate configuration objects of type {@link GeoIpResolverConfig}.
 */
public class GeoIpResolverConfigValidator implements ClusterConfigValidator {

    private static final Logger LOG = LoggerFactory.getLogger(GeoIpResolverConfigValidator.class);

    private static final List<TimeUnit> VALID_UNITS = Arrays.asList(TimeUnit.SECONDS, TimeUnit.MINUTES, TimeUnit.HOURS, TimeUnit.DAYS);

    //A test address.  This will NOT be in any database, but should only produce an
    //AddressNotFoundException.  Any other exception suggests an actual error such as
    //a database file that does not belong to the vendor selected
    private final InetAddress testAddress = InetAddress.getLoopbackAddress();
    private final GeoIpVendorResolverService geoIpVendorResolverService;
    private final GeoIpFileServiceFactory geoIpFileServiceFactory;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public GeoIpResolverConfigValidator(GeoIpVendorResolverService geoIpVendorResolverService,
                                        GeoIpFileServiceFactory geoIpFileServiceFactory,
                                        ClusterConfigService clusterConfigService) {
        this.geoIpVendorResolverService = geoIpVendorResolverService;
        this.geoIpFileServiceFactory = geoIpFileServiceFactory;
        this.clusterConfigService = clusterConfigService;
    }

    @Override
    public void validate(Object configObject) throws ConfigValidationException {

        if (configObject instanceof GeoIpResolverConfig config) {

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
        final GeoIpFileService geoIpFileService = geoIpFileServiceFactory.create(config);
        try {

            if (!VALID_UNITS.contains(config.refreshIntervalUnit())) {
                String valid = VALID_UNITS.stream().map(TimeUnit::name).collect(Collectors.joining(","));
                String error = String.format(Locale.ENGLISH, "Invalid '%s'. Valid units are '%s'", GeoIpResolverConfig.FIELD_REFRESH_INTERVAL_UNIT, valid);
                throw new IllegalArgumentException(error);
            }

            GeoIpResolverConfig curConfig = clusterConfigService.getOrDefault(GeoIpResolverConfig.class,
                    GeoIpResolverConfig.defaultConfig());
            boolean moveTemporaryFiles = false;
            if (config.useS3() && config.useGcs()) {
                throw new ConfigValidationException("Cannot use both S3 and GCS at the same time. Please choose at most one.");
            }

            if (config.useS3() || config.useGcs()) {
                //Throws ConfigValidationException if the config is invalid:
                geoIpFileService.validateConfiguration(config);

                boolean asnFileExists = !config.asnDbPath().isBlank();

                //Configuration seems to be valid, let's check if we need to download the files:
                boolean bucketsChanged = !curConfig.cityDbPath().equals(config.cityDbPath()) || !curConfig.asnDbPath().equals(config.asnDbPath());
                if (bucketsChanged || geoIpFileService.fileRefreshRequired(config)) {
                    geoIpFileService.downloadFilesToTempLocation(config);
                    config = config.toBuilder()
                            .cityDbPath(geoIpFileService.getTempCityFile())
                            .asnDbPath(asnFileExists ? geoIpFileService.getTempAsnFile() : "")
                            .build();
                    moveTemporaryFiles = true;
                } else {
                    config = config.toBuilder()
                            .cityDbPath(geoIpFileService.getActiveCityFile())
                            .asnDbPath(asnFileExists ? geoIpFileService.getActiveAsnFile() : "")
                            .build();
                }
            }

            // Validate the DB files
            validateGeoIpLocationResolver(config, timer);
            validateGeoIpAsnResolver(config, timer);

            // If the files were downloaded from the cloud and validated successfully, move the temporary files to be active
            if (moveTemporaryFiles) {
                geoIpFileService.moveTempFilesToActive();
            }
        } catch (IllegalArgumentException | IllegalStateException | CloudDownloadException | IOException e) {
            throw new ConfigValidationException(e.getMessage());
        }
    }

    public void validateGeoIpAsnResolver(GeoIpResolverConfig config, Timer timer) {
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

    public void validateGeoIpLocationResolver(GeoIpResolverConfig config, Timer timer) {
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
