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

import java.nio.file.Files;
import java.util.Objects;

public class ConfigSectionCompletenessVerifier {

    public ConfigSectionCompleteness verifyConfigSectionCompleteness(final ConfigSectionRequirements configSectionRequirements) {
        if (configSectionRequirements == null || configSectionRequirements.requirementsCount() == 0) {
            return ConfigSectionCompleteness.COMPLETE; //no requirements
        }

        final long properStringPropertiesCount = configSectionRequirements.requiredStringProperties()
                .stream()
                .filter(Objects::nonNull)
                .map(ConfigProperty::propertyValue)
                .filter(Objects::nonNull)
                .filter(value -> !value.isEmpty())
                .count();

        final long properFilePropertiesCount = configSectionRequirements.requiredFiles()
                .stream()
                .filter(Objects::nonNull)
                .filter(Files::exists)
                .count();

        final long totalRequirementsMet = properFilePropertiesCount + properStringPropertiesCount;

        if (totalRequirementsMet == 0) {
            return ConfigSectionCompleteness.MISSING;
        } else if (totalRequirementsMet == configSectionRequirements.requirementsCount()) {
            return ConfigSectionCompleteness.COMPLETE;
        }
        return ConfigSectionCompleteness.INCOMPLETE;
    }
}
