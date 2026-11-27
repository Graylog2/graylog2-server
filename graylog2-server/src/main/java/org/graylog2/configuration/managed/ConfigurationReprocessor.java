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
import com.github.joschi.jadconfig.Parameter;
import com.github.joschi.jadconfig.Repository;
import com.github.joschi.jadconfig.RepositoryException;
import com.github.joschi.jadconfig.ValidationException;
import com.github.joschi.jadconfig.info.ParameterDeclaration;
import com.github.joschi.jadconfig.info.ParameterInfo;
import com.github.joschi.jadconfig.info.ParameterInfoCollector;
import jakarta.annotation.Nullable;
import org.graylog2.configuration.ConfigurationInfoService;
import org.graylog2.shared.SuppressForbidden;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Processes the configuration beans of this node again after startup, so that a value stored through the API reaches
 * them without restarting the JVM.
 * <p>
 * Processing again writes the fields of the configuration beans. That only has an effect on code which reads a bean
 * when it needs the value: values injected by name and everything built once at startup keep what they got then.
 * <p>
 * JadConfig only writes the fields of parameters which some repository has a value for. A field whose value has gone
 * away, e.g. because a stored value has been removed or refused, would keep it, so such fields are reset to their
 * default first, as a fresh start of the JVM would leave them. Only parameters whose value can go away at runtime are
 * reset: some fields are changed by the code on purpose, e.g. {@code is_leader} when another leader is found, and
 * resetting those would undo it.
 */
public class ConfigurationReprocessor {
    private static final Logger LOG = LoggerFactory.getLogger(ConfigurationReprocessor.class);

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
     *
     * @param resettableParameters the parameters whose value can go away at runtime, which are reset to their
     *                             default when no repository has a value for them any more
     */
    public synchronized void reprocess(Set<String> resettableParameters)
            throws RepositoryException, ValidationException {
        try {
            resetParametersWithoutValue(resettableParameters);
            jadConfig.process();
        } finally {
            configurationInfoService.refresh(parameterInfoCollector.getParameterInfos());
        }
    }

    /**
     * Resets the fields of the given parameters to their default if no repository has a value for them. Parameters
     * with a value are left alone, so that processing overwrites them in one step and other threads never see their
     * default in between.
     */
    private void resetParametersWithoutValue(Set<String> resettableParameters) throws RepositoryException {
        if (resettableParameters.isEmpty()) {
            return;
        }
        final List<Repository> repositories = jadConfig.getRepositories();
        // Repositories load their values when they are opened, so without opening them again they'd still serve a
        // value which has gone away in the meantime. Processing opens them once more, which is harmless.
        for (final Repository repository : repositories) {
            repository.open();
        }
        for (final ParameterInfo parameterInfo : parameterInfoCollector.getParameterInfos().values()) {
            if (!resettableParameters.contains(parameterInfo.name())) {
                continue;
            }
            for (final ParameterDeclaration declaration : parameterInfo.declarations()) {
                resetToDefault(declaration, repositories);
            }
        }
    }

    @SuppressForbidden("JadConfig writes the private fields of configuration beans the same way")
    private void resetToDefault(ParameterDeclaration declaration, List<Repository> repositories) {
        try {
            final Field field = declaration.declaringClass().getDeclaredField(declaration.metadata().fieldName());
            final Parameter parameter = field.getAnnotation(Parameter.class);
            if (parameter == null || hasValue(repositories, parameter.value())
                    || hasValue(repositories, parameter.fallbackPropertyName())) {
                return;
            }
            final Object bean = bean(declaration.beanClass());
            if (bean == null) {
                return;
            }
            field.setAccessible(true);
            if (!Objects.equals(field.get(bean), declaration.defaultValue())) {
                LOG.debug("Resetting configuration parameter <{}> to its default.", parameter.value());
                field.set(bean, declaration.defaultValue());
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            // Processing still runs, the field just keeps its current value as it did before
            LOG.warn("Couldn't reset configuration parameter <{}> to its default.",
                    declaration.metadata().name(), e);
        }
    }

    private static boolean hasValue(List<Repository> repositories, @Nullable String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        return repositories.stream().anyMatch(repository -> repository.read(name) != null);
    }

    @Nullable
    private Object bean(Class<?> beanClass) {
        return jadConfig.getConfigurationBeans().stream()
                .filter(bean -> bean.getClass().equals(beanClass))
                .findFirst()
                .orElse(null);
    }
}
