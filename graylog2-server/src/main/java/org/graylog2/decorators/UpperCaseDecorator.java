package org.graylog2.decorators;

import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.decorators.MessageDecorator;

import java.util.Map;
import java.util.stream.Collectors;

public class UpperCaseDecorator implements MessageDecorator {
    @Override
    public ResultMessage apply(ResultMessage resultMessage) {
        final Map<String, Object> transformedFields = resultMessage.getMessage().getFields().entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                (entry) -> entry.getValue() instanceof String ? entry.getValue().toString().toUpperCase() : entry.getValue()
            ));
        resultMessage.setMessage(resultMessage.getMessage().getId(), transformedFields);
        return resultMessage;
    }
}
