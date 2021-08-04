package org.graylog.failure;

public enum ProcessingFailureCause implements FailureCause {
    RuleStatementEvaluationError("RuleStatementEvaluationError"),
    RuleConditionEvaluationError("RuleConditionEvaluationError"),
    UNKNOWN("UNKNOWN"),
    ;

    private final String label;

    ProcessingFailureCause(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
