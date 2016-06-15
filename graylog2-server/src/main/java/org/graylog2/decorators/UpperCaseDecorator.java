package org.graylog2.decorators;

import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.decorators.MessageDecorator;

import java.util.List;
import java.util.stream.Collectors;

public class UpperCaseDecorator implements MessageDecorator {
    @Override
    public List<ResultMessage> apply(List<ResultMessage> resultMessages) {
        return resultMessages;
    }
}
