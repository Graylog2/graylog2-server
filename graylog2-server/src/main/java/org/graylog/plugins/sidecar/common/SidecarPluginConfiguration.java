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
package org.graylog.plugins.sidecar.common;

import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.Validator;
import com.github.joschi.jadconfig.documentation.Documentation;
import com.github.joschi.jadconfig.util.Duration;
import com.github.joschi.jadconfig.validators.PositiveDurationValidator;
import com.github.joschi.jadconfig.validators.PositiveIntegerValidator;
import com.github.joschi.jadconfig.validators.StringNotEmptyValidator;
import org.graylog2.plugin.PluginConfigBean;

import java.util.regex.Pattern;

public class SidecarPluginConfiguration implements PluginConfigBean {
    private static final String PREFIX = "sidecar_";

    @Documentation("The name of the built-in Graylog service account a Sidecar authenticates as.")
    @Parameter(value = PREFIX + "user", validator = StringNotEmptyValidator.class)
    private String user = "graylog-sidecar";

    @Documentation("Directory holding the Linux collector binaries from the Sidecar package; used for the default " +
            "collector executable paths. Set it to match the installed packages. Applied only when the default " +
            "collectors are first created; existing collectors keep their " +
            "executable paths.")
    @Parameter(value = PREFIX + "collector_binary_dir", validator = StringNotEmptyValidator.class)
    private String collectorBinaryDir = "/usr/lib/graylog-sidecar";

    @Documentation("Default Linux collector spool directory; used as the spoolDir fallback in the default collector " +
            "configurations. Set it to match the installed packages. Applied when the default configurations are " +
            "first created.")
    @Parameter(value = PREFIX + "spool_dir", validator = StringNotEmptyValidator.class)
    private String spoolDir = "/var/lib/graylog-sidecar";

    @Documentation("Windows Sidecar installation directory; used for the default Windows executable paths and spoolDir " +
            "fallbacks. Set it to match the installed packages. Applied when the default collectors are first " +
            "created; existing collectors keep their executable paths.")
    @Parameter(value = PREFIX + "windows_install_dir", validator = StringNotEmptyValidator.class)
    private String windowsInstallDir = "C:\\Program Files\\Graylog\\sidecar";

    @Documentation("Server configuration directory monitored by the default auditbeat file-integrity config. Override " +
            "it for custom or relocated server installations. Applied when the default configuration is first " +
            "created.")
    @Parameter(value = PREFIX + "server_config_dir", validator = StringNotEmptyValidator.class)
    private String serverConfigDir = "/etc/graylog/server";

    @Documentation("Name of the configuration variable (referenced as ${user.<name>}) holding the server host. Override " +
            "it to use a different name in the default configs (e.g. custom_host). Allowed: A-Z, a-z, 0-9, _; must not " +
            "start with a digit.")
    @Parameter(value = PREFIX + "host_variable", validator = ConfigurationVariableNameValidator.class)
    private String hostVariable = "graylog_host";

    @Documentation("Expiration time for entries in the Sidecar ETag caches.")
    @Parameter(value = PREFIX + "cache_time", validator = PositiveDurationValidator.class)
    private Duration cacheTime = Duration.hours(1L);

    @Documentation("Maximum number of entries held by each of the Sidecar ETag caches.")
    @Parameter(value = PREFIX + "cache_max_size", validator = PositiveIntegerValidator.class)
    private int cacheMaxSize = 5000;

    public Duration getCacheTime() {
        return cacheTime;
    }

    public String getUser() {
        return user;
    }

    public int getCacheMaxSize() {
        return cacheMaxSize;
    }

    public String getCollectorBinaryDir() {
        return collectorBinaryDir;
    }

    public String getSpoolDir() {
        return spoolDir;
    }

    public String getWindowsInstallDir() {
        return windowsInstallDir;
    }

    public String getServerConfigDir() {
        return serverConfigDir;
    }

    public String getHostVariable() {
        return hostVariable;
    }

    /**
     * Rejects values that aren't valid configuration variable names (same rule as {@code ConfigurationVariableResource}),
     * since the value is embedded into collector templates as {@code ${user.<name>}}.
     */
    public static class ConfigurationVariableNameValidator implements Validator<String> {
        private static final Pattern VALID_NAME = Pattern.compile("^[A-Za-z_][A-Za-z0-9_]*$");

        @Override
        public void validate(String name, String value) throws ValidationException {
            if (value == null || !VALID_NAME.matcher(value).matches()) {
                throw new ValidationException("Parameter " + name + " (\"" + value + "\") may only contain the " +
                        "characters A-Z, a-z, 0-9, _ and must not start with a digit.");
            }
        }
    }
}
