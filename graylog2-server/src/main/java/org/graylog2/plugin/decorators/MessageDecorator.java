package org.graylog2.plugin.decorators;

import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;

import java.util.function.Function;

@FunctionalInterface
public interface MessageDecorator extends Function<ResultMessage, ResultMessage> {
}
