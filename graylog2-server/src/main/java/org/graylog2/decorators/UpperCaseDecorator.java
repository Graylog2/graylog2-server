package org.graylog2.decorators;

import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.decorators.MessageDecorator;

public class UpperCaseDecorator implements MessageDecorator {
    @Override
    public ResultMessage apply(ResultMessage resultMessage) {
        final Message message = resultMessage.getMessage();
        message.getFields().entrySet().stream().forEach((entry) -> message.addField(entry.getKey(), entry.getValue().toString().toUpperCase()));
        resultMessage.setMessage(message);
        return resultMessage;
    }
}
