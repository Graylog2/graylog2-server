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

import com.github.zafarkhaja.semver.Version;
import org.graylog.testing.mongodb.MongoDBVersion;
import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;

import static org.graylog2.shared.utilities.StringUtils.f;

public record EnabledIfMongoDBCondition(MongoDBVersion version) implements ExecutionCondition {
    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        final var optionalAnnotation = AnnotationSupport.findAnnotation(context.getElement(), EnabledIfMongoDB.class);
        if (optionalAnnotation.isEmpty()) {
            return ConditionEvaluationResult.enabled("No @EnabledIfMongoDB present");
        }

        final var versionRange = optionalAnnotation.get().version();
        final var mongoVersion = Version.parse(version.version(), false);

        if (mongoVersion.satisfies(versionRange)) {
            return ConditionEvaluationResult.enabled("MongoDB version range satisfied: " + versionRange);
        } else {
            return ConditionEvaluationResult.disabled(
                    f("Disabled, because MongoDB version \"%s\" does not satisfy \"%s\"",
                            version.version(), versionRange));
        }
    }
}
