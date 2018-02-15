package org.graylog.plugins.pipelineprocessor.codegen;

import org.graylog.plugins.pipelineprocessor.EvaluationContext;

public interface GeneratedRule {

    String name();

    boolean when(EvaluationContext context);

    void then(EvaluationContext context);

}
