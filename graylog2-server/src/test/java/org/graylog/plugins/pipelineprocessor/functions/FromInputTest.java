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
package org.graylog.plugins.pipelineprocessor.functions;

import com.google.common.eventbus.EventBus;
import org.antlr.v4.runtime.CommonToken;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.expressions.Expression;
import org.graylog.plugins.pipelineprocessor.ast.expressions.StringExpression;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionArgs;
import org.graylog2.plugin.IOState;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.inputs.MessageInput;
import org.graylog2.shared.inputs.InputRegistry;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * It is a valid use case to have multiple inputs with the same name. This should only happen during
 * the time a user stops an input to replace it with another input of the same name. And because of the
 * name used to reference the input in pipeline rules, this works seamlessly.
 * But it's also possible that by mistake (or because they want to), a user can have multiple inputs with
 * the same name running.
 * We want to match to Source Input ID of the current message against all valid inputs in the given moment -
 * and not only test against the first input that matches the title in the registry.
 */
class FromInputTest {
    private static final MessageFactory messageFactory = new TestMessageFactory();

    private InputRegistry inputRegistry;
    private FromInput fromInput;

    @BeforeEach
    void setUp() {
        inputRegistry = mock(InputRegistry.class);
        fromInput = new FromInput(inputRegistry);
    }

    @Test
    void matchesByName() {
        registerInputs(input("input-1", "Syslog"));

        assertThat(evaluateByName("Syslog", "input-1")).isTrue();
    }

    @Test
    void matchesByNameIgnoringCase() {
        registerInputs(input("input-1", "Syslog"));

        assertThat(evaluateByName("syslog", "input-1")).isTrue();
    }

    @Test
    void doesNotMatchByNameIfMessageComesFromDifferentInput() {
        registerInputs(input("input-1", "Syslog"), input("input-2", "GELF"));

        assertThat(evaluateByName("Syslog", "input-2")).isFalse();
    }

    @Test
    void doesNotMatchUnknownName() {
        registerInputs(input("input-1", "Syslog"));

        assertThat(evaluateByName("Unknown", "input-1")).isFalse();
    }

    @Test
    void matchesEveryInputWithSameName() {
        registerInputs(input("input-1", "Syslog"), input("input-2", "syslog"), input("input-3", "Syslog"));

        assertThat(evaluateByName("Syslog", "input-1")).isTrue();
        assertThat(evaluateByName("Syslog", "input-2")).isTrue();
        assertThat(evaluateByName("Syslog", "input-3")).isTrue();
    }

    @Test
    void doesNotMatchOtherInputIfMultipleInputsShareName() {
        registerInputs(input("input-1", "Syslog"), input("input-2", "Syslog"), input("input-3", "GELF"));

        assertThat(evaluateByName("Syslog", "input-3")).isFalse();
    }

    @Test
    void returnsNullWithoutIdAndName() {
        registerInputs(input("input-1", "Syslog"));

        assertThat(evaluate(Map.of(), "input-1")).isNull();
    }

    @Test
    void matchesById() {
        final MessageInput input = input("input-1", "Syslog");
        registerInputs(input);
        when(inputRegistry.getInputState("input-1")).thenReturn(ioState(input));

        assertThat(evaluate(Map.of(FromInput.ID_ARG, string("input-1")), "input-1")).isTrue();
        assertThat(evaluate(Map.of(FromInput.ID_ARG, string("input-1")), "input-2")).isFalse();
    }

    @Test
    void doesNotMatchUnknownId() {
        registerInputs(input("input-1", "Syslog"));

        assertThat(evaluate(Map.of(FromInput.ID_ARG, string("unknown")), "input-1")).isFalse();
    }

    private Boolean evaluateByName(String name, String sourceInputId) {
        return evaluate(Map.of(FromInput.NAME_ARG, string(name)), sourceInputId);
    }

    private Boolean evaluate(Map<String, Expression> args, String sourceInputId) {
        final Message message = messageFactory.createMessage("test", "test", DateTime.parse("2010-07-30T16:03:25Z"));
        message.setSourceInputId(sourceInputId);
        return fromInput.evaluate(new FunctionArgs(fromInput, args), new EvaluationContext(message));
    }

    private void registerInputs(MessageInput... inputs) {
        final Set<IOState<MessageInput>> states = new HashSet<>();
        for (MessageInput input : inputs) {
            states.add(ioState(input));
        }
        when(inputRegistry.getInputStates()).thenReturn(states);
    }

    private static IOState<MessageInput> ioState(MessageInput input) {
        return new IOState<>(new EventBus(), input, IOState.Type.RUNNING);
    }

    private static MessageInput input(String id, String title) {
        final MessageInput input = mock(MessageInput.class);
        when(input.getId()).thenReturn(id);
        when(input.getTitle()).thenReturn(title);
        return input;
    }

    private static StringExpression string(String value) {
        return new StringExpression(new CommonToken(0), value);
    }
}
