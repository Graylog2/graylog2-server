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
package org.graylog.plugins.views.search.validation.subvalidators;

import com.google.common.collect.Streams;
import org.graylog.plugins.views.search.validation.ParsedTerm;
import org.graylog.plugins.views.search.validation.ValidationMessage;
import org.graylog.plugins.views.search.validation.ValidationType;

import javax.inject.Singleton;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
public class ValidationExplanationCreator {

    public List<ValidationMessage> getExceptionExplanations(final Exception e) {
        return Collections.singletonList(ValidationMessage.fromException(e));
    }

    public List<ValidationMessage> getVerificationExplanations(final List<ParsedTerm> unknownFields,
                                                               final List<ParsedTerm> invalidOperators) {

        final Stream<ValidationMessage> unknownFieldsStream = unknownFields.stream().map(term -> {
            final ValidationMessage.Builder messageBuilder = getErrorMessageBuilder((t) -> "Query contains unknown field: " + term.getRealFieldName(),
                    ValidationType.UNKNOWN_FIELD,
                    term);
            messageBuilder.relatedProperty(term.getRealFieldName());
            return messageBuilder.build();
        });

        final Stream<ValidationMessage> invalidOperatorsStream = invalidOperators.stream().map(term -> {
            final ValidationMessage.Builder message = getErrorMessageBuilder((t) -> String.format(Locale.ROOT, "Query contains invalid operator \"%s\". All AND / OR / NOT operators have to be written uppercase", t.value()),
                    ValidationType.INVALID_OPERATOR,
                    term);
            return message.build();
        });

        return Streams.concat(unknownFieldsStream, invalidOperatorsStream)
                .distinct()
                .collect(Collectors.toList());
    }

    private ValidationMessage.Builder getErrorMessageBuilder(final Function<ParsedTerm, String> errorMessageFunction, final ValidationType validationType, final ParsedTerm parsedTerm) {
        final ValidationMessage.Builder message = ValidationMessage.builder(validationType)
                .errorMessage(errorMessageFunction.apply(parsedTerm));

        parsedTerm.keyToken().ifPresent(t -> {
            message.beginLine(t.beginLine());
            message.beginColumn(t.beginColumn());
            message.endLine(t.endLine());
            message.endColumn(t.endColumn());
        });
        return message;
    }
}
