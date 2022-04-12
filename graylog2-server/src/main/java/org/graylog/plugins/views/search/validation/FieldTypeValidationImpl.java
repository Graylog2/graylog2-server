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

import com.google.common.net.InetAddresses;
import org.joda.time.DateTime;

import javax.inject.Singleton;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public class FieldTypeValidationImpl implements FieldTypeValidation {

    private static final Map<String, Predicate<String>> VALIDATION_FUNCTIONS = new HashMap<>();

    private static final Predicate<String> ALWAYS_TRUE_PREDICATE = value -> true;

    static {
        VALIDATION_FUNCTIONS.put("string", ALWAYS_TRUE_PREDICATE);
        VALIDATION_FUNCTIONS.put("long", wrapException(Long::parseLong));
        VALIDATION_FUNCTIONS.put("int", wrapException(Integer::parseInt));
        VALIDATION_FUNCTIONS.put("short", wrapException(Short::parseShort));
        VALIDATION_FUNCTIONS.put("byte", wrapException(Byte::parseByte));
        VALIDATION_FUNCTIONS.put("double", wrapException(Double::parseDouble));
        VALIDATION_FUNCTIONS.put("float", wrapException(Float::parseFloat));
        VALIDATION_FUNCTIONS.put("date", wrapException(DateTime::parse));
        VALIDATION_FUNCTIONS.put("boolean", wrapException(Boolean::parseBoolean));
        VALIDATION_FUNCTIONS.put("binary", ALWAYS_TRUE_PREDICATE);
        VALIDATION_FUNCTIONS.put("geo-point", ALWAYS_TRUE_PREDICATE);
        VALIDATION_FUNCTIONS.put("ip", InetAddresses::isInetAddress);
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
    public Optional<ValidationMessage> validateFieldValueType(ParsedTerm t, String detectedFieldType) {
        if (!typeMatching(detectedFieldType, t.value())) {
            final ValidationMessage.Builder builder = ValidationMessage.builder(ValidationType.INVALID_VALUE_TYPE)
                    .errorMessage(String.format(Locale.ROOT, "Type of %s is %s, cannot use value %s", t.getRealFieldName(), detectedFieldType, t.value()));

            // prefer value token, accept key token as fallback
            Optional<ImmutableToken> tokenWithPositions = t.valueToken().isPresent() ? t.valueToken() : t.keyToken();
            tokenWithPositions.ifPresent(token -> {
                builder.beginLine(token.beginLine());
                builder.beginColumn(token.beginColumn());
                builder.endLine(token.endLine());
                builder.endColumn(token.endColumn());
            });
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
