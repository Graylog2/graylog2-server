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
package org.graylog2.configuration.managed;

import com.github.joschi.jadconfig.JadConfig;
import com.github.joschi.jadconfig.Repository;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.info.ParameterInfoCollector;
import org.graylog2.configuration.ConfigurationInfoService;

import java.util.ArrayList;
import java.util.List;

/**
 * Processes the configuration beans of this node again after startup, so that a value stored through the API reaches
 * them without restarting the JVM.
 * <p>
 * Processing again writes the fields of the configuration beans. That only has an effect on code which reads a bean
 * when it needs the value: values injected by name and everything built once at startup keep what they got then.
 */
public class ConfigurationReprocessor {
    private final JadConfig jadConfig;
    private final ParameterInfoCollector parameterInfoCollector;
    private final ConfigurationInfoService configurationInfoService;

    public ConfigurationReprocessor(JadConfig jadConfig,
                                    ParameterInfoCollector parameterInfoCollector,
                                    ConfigurationInfoService configurationInfoService) {
        this.jadConfig = jadConfig;
        this.parameterInfoCollector = parameterInfoCollector;
        this.configurationInfoService = configurationInfoService;
    }

    /**
     * Puts a repository in front of all others, so its values win. Does nothing if it has been added before.
     */
    public synchronized void prependRepository(Repository repository) {
        final List<Repository> current = jadConfig.getRepositories();
        if (current.contains(repository)) {
            return;
        }
        final List<Repository> repositories = new ArrayList<>(current.size() + 1);
        repositories.add(repository);
        repositories.addAll(current);
        jadConfig.setRepositories(repositories);
    }

    /**
     * Processes all configuration beans again and updates the {@link ConfigurationInfoService} with the outcome,
     * also if processing failed halfway: the beans then hold a mix of old and new values, which the information
     * should reflect.
     */
    public synchronized void reprocess() throws RepositoryException, ValidationException {
        try {
            jadConfig.process();
        } finally {
            configurationInfoService.refresh(parameterInfoCollector.getParameterInfos());
        }
    }
}
