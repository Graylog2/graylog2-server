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
package org.graylog.datanode.configuration.verification;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public record ConfigSectionRequirements(Collection<ConfigProperty> requiredStringProperties,
                                        Collection<Path> requiredFiles) {

    public ConfigSectionRequirements(Collection<ConfigProperty> requiredStringProperties, Collection<Path> requiredFiles) {
        this.requiredStringProperties = requiredStringProperties != null ? requiredStringProperties : List.of();
        this.requiredFiles = requiredFiles != null ? requiredFiles : List.of();
    }

    public int requirementsCount() {
        return requiredFiles().size() + requiredStringProperties().size();
    }

    public List<String> requirementsList() {
        List<String> result = new ArrayList<>(requiredStringProperties().size() + requiredFiles().size());
        requiredStringProperties().forEach(p -> result.add(p.propertyName() + "(config property)"));
        requiredFiles().forEach(f -> result.add(f.toString() + "(required file)"));
        return result;
    }
}
