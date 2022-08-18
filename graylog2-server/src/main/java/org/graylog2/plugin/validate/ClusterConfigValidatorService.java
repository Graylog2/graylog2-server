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

package org.graylog2.plugin.validate;

import javax.inject.Inject;
import java.util.Map;

public class ClusterConfigValidatorService {

    private final Map<Class<?>, ClusterConfigValidator> validators;

    @Inject
    public ClusterConfigValidatorService(Map<Class<?>, ClusterConfigValidator> validators) {
        this.validators = validators;
    }

    public void validate(Object configObject) throws ConfigValidationException {
        if (configObject == null) {
            return;
        }

        try {
            Class<?> zclass = configObject.getClass();
            ClusterConfigValidator validator = validators.get(zclass);
            if (validator == null) {
                //try parent class--the config object is likely an AutoValue generated class which extends the
                //registered class.
                Class<?> zParent = zclass.getSuperclass();
                validator = validators.getOrDefault(zParent, obj -> {});
            }
            validator.validate(configObject);

        } catch (RuntimeException e) {
            throw new ConfigValidationException(e.getMessage());
        }
    }
}
