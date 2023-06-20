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
package org.graylog.security.certutil;

import com.github.joschi.jadconfig.Parameter;
import org.graylog2.plugin.BaseConfiguration;

import java.nio.file.Path;

public class CaConfiguration extends BaseConfiguration {
    @Parameter(value = "ca_keystore_file")
    private Path caKeystoreFile;

    @Parameter(value = "ca_password")
    private String caPassword;

    public Path getCaKeystoreFile() {
        return caKeystoreFile;
    }

    public String getCaPassword() {
        return caPassword;
    }

    public boolean configuredCaExists() {
        return getCaKeystoreFile() != null;
    }
}
