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
package org.graylog.plugins.views.search.validation;

import com.google.common.collect.ImmutableList;
import com.google.common.net.InetAddresses;
import org.graylog.plugins.views.search.engine.QueryPosition;
import org.graylog2.plugin.Tools;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FieldTypeValidationImpl implements FieldTypeValidation {

    private static final List<DateTimeFormatter> DATE_TIME_FORMATTERS = ImmutableList.of(
            Tools.ISO_DATE_FORMAT_FORMATTER,
            Tools.ES_DATE_FORMAT_FORMATTER,
            Tools.ES_DATE_FORMAT_NO_MS_FORMATTER,
            ISODateTimeFormat.dateTimeParser().withOffsetParsed());

    private static final List<String> NUMERIC_OPERANDS = ImmutableList.of(">=", "<=", ">", "<").stream()
            .sorted(Comparator.comparingInt(String::length).reversed())
            .collect(Collectors.toList());
    private static final Map<String, Predicate<String>> VALIDATION_FUNCTIONS = new HashMap<>();

    private static final Predicate<String> ALWAYS_TRUE_PREDICATE = value -> true;

    static {
        VALIDATION_FUNCTIONS.put("string", ALWAYS_TRUE_PREDICATE);
        VALIDATION_FUNCTIONS.put("long", wrapException(removeNumericOperandsIfNeeded(Long::parseLong)));
        VALIDATION_FUNCTIONS.put("int", wrapException(removeNumericOperandsIfNeeded(Integer::parseInt)));
        VALIDATION_FUNCTIONS.put("short", wrapException(removeNumericOperandsIfNeeded(Short::parseShort)));
        VALIDATION_FUNCTIONS.put("byte", wrapException(removeNumericOperandsIfNeeded(Byte::parseByte)));
        VALIDATION_FUNCTIONS.put("double", wrapException(removeNumericOperandsIfNeeded(Double::parseDouble)));
        VALIDATION_FUNCTIONS.put("float", wrapException(removeNumericOperandsIfNeeded(Float::parseFloat)));
        VALIDATION_FUNCTIONS.put("date", FieldTypeValidationImpl::isDate);
        VALIDATION_FUNCTIONS.put("boolean", wrapException(Boolean::parseBoolean));
        VALIDATION_FUNCTIONS.put("binary", ALWAYS_TRUE_PREDICATE);
        VALIDATION_FUNCTIONS.put("geo-point", ALWAYS_TRUE_PREDICATE);
        VALIDATION_FUNCTIONS.put("ip", InetAddresses::isInetAddress);
    }

    private static Function<String, Object> removeNumericOperandsIfNeeded(Function<String, Object> numericParser) {
        return (value) -> {
            final Optional<String> operand = NUMERIC_OPERANDS.stream()
                    .filter(value::startsWith)
                    .findFirst();
            if (operand.isPresent()) {
                return numericParser.apply(value.substring(operand.get().length()));
            } else {
                return numericParser.apply(value);
            }
        };
    }


    private static boolean isDate(final String dateCandidate) {
        for (DateTimeFormatter formatter : DATE_TIME_FORMATTERS) {
            try {
                formatter.parseDateTime(dateCandidate);
                return true;
            } catch (Exception ex) {
                //do nothing, try next formatter in the loop
            }
        }
        return false;
    }

    @SuppressWarnings("ReturnValueIgnored")
    private static Predicate<String> wrapException(Function<String, Object> parser) {
        return (value) -> {
            try {
                parser.apply(value);
                return true;
            } catch (Exception e) {
                return false;
            }
        };
    }

    @Override
    public Optional<ValidationMessage> validateFieldValueType(ParsedTerm term, String detectedFieldType) {
        if (!typeMatching(detectedFieldType, term.value())) {
            final ValidationMessage.Builder builder = ValidationMessage.builder(ValidationStatus.WARNING, ValidationType.INVALID_VALUE_TYPE)
                    .errorMessage(String.format(Locale.ROOT, "Type of %s is %s, cannot use value %s", term.getRealFieldName(), detectedFieldType, term.value()));

            // prefer value token, accept key token as fallback
            Optional<ImmutableToken> tokenWithPositions = term.valueToken().isPresent() ? term.valueToken() : term.keyToken();

            tokenWithPositions
                    .map(QueryPosition::from)
                    .ifPresent(builder::position);

            return Optional.of(builder.build());
        }
        return Optional.empty();
    }

    private boolean typeMatching(String type, String value) {
        return Optional.ofNullable(type)
                .map(VALIDATION_FUNCTIONS::get)
                .map(validator -> validator.test(value))
                .orElse(true);
    }
}
