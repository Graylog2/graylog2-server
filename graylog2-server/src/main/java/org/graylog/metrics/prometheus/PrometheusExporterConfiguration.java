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
package org.graylog.metrics.prometheus;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.guava.converters.HostAndPortConverter;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.FilePathReadableValidator;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.google.common.net.HostAndPort;

import java.nio.file.Path;
import java.nio.file.Paths;

@SuppressWarnings({"FieldMayBeFinal", "unused"})
public class PrometheusExporterConfiguration {
    private static final String PREFIX = "prometheus_exporter_";

    public static final String ENABLED = PREFIX + "enabled";
    public static final String BIND_ADDRESS = PREFIX + "bind_address";
    public static final String MAPPING_FILE_PATH_CORE = PREFIX + "mapping_file_path_core";
    public static final String MAPPING_FILE_PATH_CUSTOM = PREFIX + "mapping_file_path_custom";
    public static final String MAPPING_FILE_REFRESH_INTERVAL = PREFIX + "mapping_file_refresh_interval";

    private static String DEFAULT_BIND_ADDRESS_HOST = "127.0.0.1";
    // TODO: Grab a default port from https://github.com/prometheus/prometheus/wiki/Default-port-allocations once
    //       we have working code and link to the repository or the documentation.
    //       Check if we really want this or if we consider this a private reporter and should choose a default port
    //       outside of the "official" Prometheus exporter range.
    private static int DEFAULT_BIND_ADDRESS_PORT = 9832;

    @Parameter(value = ENABLED, required = true)
    private boolean enabled = false;

    @Parameter(value = BIND_ADDRESS, converter = CustomHostAndPortConverter.class)
    private HostAndPort bindAddress = HostAndPort.fromParts(DEFAULT_BIND_ADDRESS_HOST, DEFAULT_BIND_ADDRESS_PORT);

    @Parameter(value = MAPPING_FILE_PATH_CORE, validators = {FilePathReadableValidator.class})
    private Path mappingFilePathCore = Paths.get("prometheus-exporter-mapping-core.yml");

    @Parameter(value = MAPPING_FILE_PATH_CUSTOM, validators = {FilePathReadableValidator.class})
    private Path mappingFilePathCustom = Paths.get("prometheus-exporter-mapping-custom.yml");

    @Parameter(value = MAPPING_FILE_REFRESH_INTERVAL, validators = {PositiveDurationValidator.class})
    private Duration mappingFileRefreshInterval = Duration.seconds(60);

    public static class CustomHostAndPortConverter extends HostAndPortConverter {
        @Override
        public HostAndPort convertFrom(String value) {
            return super.convertFrom(value)
                    .requireBracketsForIPv6()
                    .withDefaultPort(DEFAULT_BIND_ADDRESS_PORT);
        }
    }
}
