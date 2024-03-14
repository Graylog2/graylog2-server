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
package org.graylog2.configuration;

import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import org.junit.Test;

import java.util.HashMap;

import static org.graylog2.configuration.ConfigurationHelper.DATA_DIR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PathConfigurationTest {

    public static final String BIN_PATH = "bin";
    public static final String PLUGINS_PATH = "plugins";

    @Test
    public void testBaseConfiguration() throws ValidationException, RepositoryException {
        HashMap<String, String> properties = new HashMap<>();
        properties.put("bin_dir", BIN_PATH);
        properties.put("plugin_dir", PLUGINS_PATH);
        PathConfiguration configuration = ConfigurationHelper.initPathConfig(new PathConfiguration(), properties);


        assertEquals(BIN_PATH, configuration.getBinDir().toString());
        assertEquals(DATA_DIR, configuration.getDataDir());
        assertEquals(PLUGINS_PATH, configuration.getPluginDir().toString());
        assertTrue(configuration.getAllowedAuxiliaryPaths().isEmpty());
    }

    @Test
    public void testAllowedAuxiliaryPaths() throws ValidationException, RepositoryException {
        HashMap<String, String> validProperties = new HashMap<>();
        validProperties.put("allowed_auxiliary_paths", "/permitted-dir,/another-valid-dir");

        PathConfiguration configuration = ConfigurationHelper.initPathConfig(new PathConfiguration(), validProperties);

        assertEquals(2,configuration.getAllowedAuxiliaryPaths().size());
    }
}
