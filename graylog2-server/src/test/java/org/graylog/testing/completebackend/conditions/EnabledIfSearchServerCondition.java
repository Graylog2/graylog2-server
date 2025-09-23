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
package org.graylog.testing.completebackend.conditions;

import org.graylog2.storage.SearchVersion;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.graylog2.shared.utilities.StringUtils.f;

public record EnabledIfSearchServerCondition(SearchVersion searchVersion) implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        final var optionalAnnotation = AnnotationSupport.findAnnotation(context.getElement(), EnabledIfSearchServer.class);
        if (optionalAnnotation.isEmpty()) {
            return ConditionEvaluationResult.enabled("No @EnabledIfSearchServer present");
        }

        final var annotation = optionalAnnotation.get();
        final var distribution = annotation.distribution();

        if (searchVersion.distribution() != distribution) {
            return ConditionEvaluationResult.disabled(f("Disabled, because distribution is \"%s\", but required is \"%s\"",
                    searchVersion.distribution(), distribution));
        }

        if (distribution == SearchVersion.Distribution.DATANODE) {
            return ConditionEvaluationResult.enabled(null);
        } else if (isBlank(annotation.version())) {
            throw new IllegalArgumentException("@EnabledIfSearchServer must have a value for \"version\", unless distribution is \"DATANODE\"");
        }

        final var versionRange = annotation.version();
        if (searchVersion.version().satisfies(versionRange)) {
            return ConditionEvaluationResult.enabled("Version range satisfied: " + versionRange);
        } else {
            return ConditionEvaluationResult.disabled(f("Disabled, because version \"%s\" does not satisfy \"%s\"",
                    searchVersion.version(), versionRange));
        }
    }
}
