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

import static org.graylog2.configuration.ConfigurationHelper.DEFAULT_PATH_DIR;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PathConfigurationTest {

    @Test
    public void testBaseConfiguration() throws ValidationException, RepositoryException {
        PathConfiguration configuration = ConfigurationHelper.initPathConfig(new PathConfiguration());

        assertEquals(DEFAULT_PATH_DIR, configuration.getBinDir());
        assertEquals(DEFAULT_PATH_DIR, configuration.getDataDir());
        assertEquals(DEFAULT_PATH_DIR, configuration.getPluginDir());
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
