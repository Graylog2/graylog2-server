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
import org.graylog.plugins.map.config.S3DownloadException;
import org.graylog.plugins.map.config.S3GeoIpFileService;
import org.graylog.plugins.map.geoip.GeoAsnInformation;
import org.graylog.plugins.map.geoip.GeoIpResolver;
import org.graylog.plugins.map.geoip.GeoIpVendorResolverService;
import org.graylog.plugins.map.geoip.GeoLocationInformation;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.graylog2.plugin.validate.ClusterConfigValidator;
import org.graylog2.plugin.validate.ConfigValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
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
    private final S3GeoIpFileService s3GeoIpFileService;
    private final ClusterConfigService clusterConfigService;

    @Inject
    public GeoIpResolverConfigValidator(GeoIpVendorResolverService geoIpVendorResolverService,
                                        S3GeoIpFileService s3GeoIpFileService,
                                        ClusterConfigService clusterConfigService) {
        this.geoIpVendorResolverService = geoIpVendorResolverService;
        this.s3GeoIpFileService = s3GeoIpFileService;
        this.clusterConfigService = clusterConfigService;
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

            if (!VALID_UNITS.contains(config.refreshIntervalUnit())) {
                String valid = VALID_UNITS.stream().map(TimeUnit::name).collect(Collectors.joining(","));
                String error = String.format(Locale.ENGLISH, "Invalid '%s'. Valid units are '%s'", GeoIpResolverConfig.FIELD_REFRESH_INTERVAL_UNIT, valid);
                throw new IllegalArgumentException(error);
            }

            GeoIpResolverConfig curConfig = clusterConfigService.getOrDefault(GeoIpResolverConfig.class,
                    GeoIpResolverConfig.defaultConfig());
            boolean moveTemporaryFiles = false;
            if (config.useS3()) {
                if (s3GeoIpFileService.s3ClientIsNull()) {
                    throw new ConfigValidationException("Unable to use S3 for file refresh without AWS credentials. See documentation for steps to properly configure AWS credentials.");
                }
                // Make sure the paths are valid S3 object paths
                boolean asnFileExists = !config.asnDbPath().isEmpty();
                if (config.cityDbPath().startsWith(S3GeoIpFileService.S3_BUCKET_PREFIX) &&
                        (!asnFileExists || config.asnDbPath().startsWith(S3GeoIpFileService.S3_BUCKET_PREFIX))) {
                    boolean bucketsChanged = !curConfig.cityDbPath().equals(config.cityDbPath()) || !curConfig.asnDbPath().equals(config.asnDbPath());
                    if (bucketsChanged) {
                        s3GeoIpFileService.downloadFilesToTempLocation(config);
                        // Set the DB paths to the temp file locations so the newly downloaded files can be validated
                        config = config.toBuilder()
                                .cityDbPath(s3GeoIpFileService.getTempCityFile())
                                .asnDbPath(asnFileExists ? s3GeoIpFileService.getTempAsnFile() : "")
                                .build();
                        moveTemporaryFiles = true;
                    } else {
                        // If neither of the paths have changed, don't worry about downloading the DB files. The
                        // GeoIpDbFileChangeMonitorService will handle syncing the files if necessary
                        config = config.toBuilder()
                                .cityDbPath(s3GeoIpFileService.getActiveCityFile())
                                .asnDbPath(asnFileExists ? s3GeoIpFileService.getActiveAsnFile() : "")
                                .build();
                    }
                } else {
                    throw new ConfigValidationException("Database file paths must be valid S3 object paths when using S3.");
                }
            }

            // Validate the DB files
            validateGeoIpLocationResolver(config, timer);
            validateGeoIpAsnResolver(config, timer);

            // If the files were downloaded from S3 and validated successfully, move the temporary files to be active
            if (moveTemporaryFiles) {
                s3GeoIpFileService.moveTempFilesToActive();
            }
        } catch (IllegalArgumentException | IllegalStateException | S3DownloadException | IOException e) {
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
