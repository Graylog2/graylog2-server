package org.graylog2.plugin.decorators;

import org.graylog2.indexer.results.ResultMessage;

import java.util.function.Function;

@FunctionalInterface
public interface MessageDecorator extends Function<ResultMessage, ResultMessage> {
}
