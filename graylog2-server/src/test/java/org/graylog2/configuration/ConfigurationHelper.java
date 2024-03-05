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

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.repositories.InMemoryRepository;
import org.graylog2.Configuration;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class ConfigurationHelper {
    public final static Path DATA_DIR = Paths.get(System.getProperty("java.io.tmpdir"));

    public static <T extends PathConfiguration> T initPathConfig(T config) throws ValidationException, RepositoryException {
        return initPathConfig(config, new HashMap<>());
    }

    public static <T extends Configuration> T initConfig(T config) throws ValidationException, RepositoryException {
        return initConfig(config, new HashMap<>());
    }

    public static <T extends Configuration> T initConfig(T config, Map<String, String> properties) throws ValidationException, RepositoryException {
        Map<String, String> props = new HashMap<>();
        props.put("password_secret", "ipNUnWxmBLCxTEzXcyamrdy0Q3G7HxdKsAvyg30R9SCof0JydiZFiA3dLSkRsbLF");
        props.put("root_password_sha2", "8c6976e5b5410415bde908bd4dee15dfb167a9c873fc4bb8a81f6f2ab448a918");
        props.putAll(properties);

        return initPathConfig(config, props);
    }

    public static <T extends PathConfiguration> T initPathConfig(T config, Map<String, String> properties) throws RepositoryException, ValidationException {
        Map<String, String> props = new HashMap<>();
        props.put("data_dir", DATA_DIR.toString());
        props.putAll(properties);

        final JadConfig jadConfig = new JadConfig(new InMemoryRepository(props), config);
        jadConfig.process();
        return config;
    }
}
