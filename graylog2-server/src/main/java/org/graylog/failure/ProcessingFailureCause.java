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
package org.graylog.failure;

public enum ProcessingFailureCause implements FailureCause {
    RuleStatementEvaluationError("RuleStatementEvaluationError"),
    RuleConditionEvaluationError("RuleConditionEvaluationError"),
    ExtractorException("ExtractorException"),
    MessageFilterException("MessageFilterException"),
    InvalidTimestampException("InvalidTimestampException"),
    UNKNOWN("UNKNOWN"),
    ;

    private final String label;

    ProcessingFailureCause(String label) {
        this.label = label;
    }

    @Override
    public String label() {
        return label;
    }
}
