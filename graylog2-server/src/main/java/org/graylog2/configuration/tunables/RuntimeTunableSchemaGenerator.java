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
package org.graylog2.configuration.tunables;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.victools.jsonschema.generator.CustomDefinition;
import com.github.victools.jsonschema.generator.MemberScope;
import com.github.victools.jsonschema.generator.Option;
import com.github.victools.jsonschema.generator.SchemaGenerator;
import com.github.victools.jsonschema.generator.SchemaGeneratorConfigPart;
import jakarta.annotation.Nullable;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.graylog.jsonschema.GraylogSchemaGeneratorConfig;
import org.graylog2.security.encryption.EncryptedValue;
import org.joda.time.Period;
import org.threeten.extra.PeriodDuration;

import java.lang.reflect.Field;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.StreamSupport;

import static org.graylog2.shared.utilities.StringUtils.f;

@Singleton
public class RuntimeTunableSchemaGenerator {
    public static final Set<Class<?>> DURATION_TYPES = Set.of(Duration.class, Period.class, PeriodDuration.class);
    private static final String ALLOWED_VALUES = "x-graylog-allowed-values";
    private static final List<ChronoUnit> DURATION_UNITS = List.of(ChronoUnit.SECONDS, ChronoUnit.MINUTES, ChronoUnit.HOURS, ChronoUnit.DAYS);

    private final ObjectMapper objectMapper;
    private final JacksonProperties jacksonProperties;
    private final SchemaGenerator generator;

    @Inject
    public RuntimeTunableSchemaGenerator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.jacksonProperties = new JacksonProperties(objectMapper);
        final var builder = GraylogSchemaGeneratorConfig.baseBuilder(objectMapper)
                .with(Option.INLINE_ALL_SCHEMAS, Option.FLATTENED_OPTIONALS);
        builder.forTypesInGeneral()
                .withCustomDefinitionProvider((javaType, context) -> {
                    final Class<?> raw = javaType.getErasedType();
                    if (DURATION_TYPES.contains(raw)) {
                        return new CustomDefinition(objectMapper.createObjectNode().put("type", "string").put("format", "duration"));
                    }
                    if (raw == EncryptedValue.class) {
                        return new CustomDefinition(objectMapper.createObjectNode()
                                .put("type", "string")
                                .put("format", "password")
                                .put("writeOnly", true)
                                .put("x-graylog-secret", true));
                    }
                    return null;
                });
        builder.forTypesInGeneral().withPropertySorter(Comparator.comparingInt(this::jacksonPosition));
        builder.forTypesInGeneral().withTypeAttributeOverride((node, scope, context) -> {
            final ObjectNode labels = enumLabels(scope.getType().getErasedType());
            if (labels != null) {
                node.set("x-graylog-enum-labels", labels);
            }
        });
        configureMembers(builder.forFields());
        configureMembers(builder.forMethods());
        this.generator = new SchemaGenerator(builder.build());
    }

    public ObjectNode generate(Class<?> type) throws UnsupportedTunableSchemaException {
        final ObjectNode schema = generator.generateSchema(type);
        applyFieldExtensions(schema);
        RuntimeTunableSchemaChecker.normalizeAndCheck(schema);
        return schema;
    }

    private <M extends MemberScope<?, ?>> void configureMembers(SchemaGeneratorConfigPart<M> part) {
        part.withTitleResolver(member -> {
                    final TunableField field = tunableField(member);
                    return field == null ? null : field.label();
                })
                .withDescriptionResolver(member -> {
                    final TunableField field = tunableField(member);
                    return field == null || field.help().isBlank() ? null : field.help();
                })
                .withNullableCheck(member -> member.getDeclaredType().getErasedType() == Optional.class ? Boolean.TRUE : null)
                .withInstanceAttributeOverride((node, member, context) -> addExtensions(node, member));
    }

    private void addExtensions(ObjectNode node, MemberScope<?, ?> member) {
        final ResolvedType type = valueType(member);
        final TunableField field = tunableField(member);
        if (type != null && DURATION_TYPES.contains(type.getErasedType())) {
            addUnits(node, field == null ? ChronoUnit.SECONDS : field.minUnit());
        }
        if (field == null) {
            return;
        }
        node.put("x-graylog-order", field.order());
        if (field.enabledWhen().length > 0) {
            node.set("x-graylog-enabled-when", conditions(field.enabledWhen()));
        }
        if (field.visibleWhen().length > 0) {
            node.set("x-graylog-visible-when", conditions(field.visibleWhen()));
        }
        if (field.hidden()) {
            node.put("x-graylog-hidden", true);
        }
        if (field.allowedValues().length > 0) {
            final ArrayNode allowed = node.putArray(ALLOWED_VALUES);
            Arrays.stream(field.allowedValues()).forEach(allowed::add);
        }
    }

    private void addUnits(ObjectNode node, ChronoUnit minUnit) {
        final List<String> units = DURATION_UNITS.stream()
                .filter(unit -> unit.compareTo(minUnit) >= 0)
                .map(unit -> unit.name().toLowerCase(Locale.ROOT))
                .toList();
        if (units.isEmpty()) {
            node.put("x-graylog-unsupported", f("no supported duration units from %s", minUnit));
            return;
        }
        final ArrayNode unitsNode = node.putArray("x-graylog-units");
        units.forEach(unitsNode::add);
    }

    // Field attributes that depend on the type's schema are applied once the schema is complete.
    private void applyFieldExtensions(JsonNode node) {
        if (node instanceof ObjectNode object) {
            if (object.has(ALLOWED_VALUES)) {
                final List<String> allowed = new ArrayList<>();
                object.remove(ALLOWED_VALUES).forEach(value -> allowed.add(value.asText()));
                restrictEnum(object, allowed);
            }
        }
        node.forEach(this::applyFieldExtensions);
    }

    private void restrictEnum(ObjectNode node, List<String> allowed) {
        final JsonNode values = node.has("anyOf")
                ? StreamSupport.stream(node.get("anyOf").spliterator(), false).filter(branch -> branch.has("enum")).findFirst().orElse(null)
                : node;
        if (values == null || !values.has("enum")) {
            node.put("x-graylog-unsupported", "allowedValues requires an enum");
            return;
        }
        final ArrayNode restricted = objectMapper.createArrayNode();
        values.get("enum").forEach(value -> {
            if (value.isNull() || allowed.contains(value.asText())) {
                restricted.add(value);
            }
        });
        ((ObjectNode) values).set("enum", restricted);
    }

    @Nullable
    private ObjectNode enumLabels(Class<?> type) {
        if (!type.isEnum()) {
            return null;
        }
        final ObjectNode labels = objectMapper.createObjectNode();
        for (Object constant : type.getEnumConstants()) {
            final TunableEnumLabel label = enumField(type, (Enum<?>) constant).getAnnotation(TunableEnumLabel.class);
            if (label != null) {
                labels.put(objectMapper.valueToTree(constant).asText(), label.value());
            }
        }
        return labels.isEmpty() ? null : labels;
    }

    private static Field enumField(Class<?> type, Enum<?> constant) {
        try {
            return type.getField(constant.name());
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(f("Enum constant %s.%s has no field", type.getSimpleName(), constant.name()), e);
        }
    }

    private ArrayNode conditions(When[] conditions) {
        final ArrayNode result = objectMapper.createArrayNode();
        for (When when : conditions) {
            final ObjectNode condition = result.addObject().put("property", when.property());
            final ArrayNode in = condition.putArray("in");
            Arrays.stream(when.in()).forEach(in::add);
        }
        return result;
    }

    private int jacksonPosition(MemberScope<?, ?> member) {
        final String name = member.getSchemaPropertyName();
        final List<String> names = jacksonProperties.properties(member.getDeclaringType().getErasedType()).stream()
                .map(BeanPropertyDefinition::getName)
                .toList();
        final int position = names.indexOf(name);

        return position < 0 ? Integer.MAX_VALUE : position;
    }

    private static ResolvedType valueType(MemberScope<?, ?> member) {
        final ResolvedType type = member.getType();
        if (type != null && type.getErasedType() == Optional.class && !type.getTypeParameters().isEmpty()) {
            return type.getTypeParameters().get(0);
        }
        return type;
    }

    private static TunableField tunableField(MemberScope<?, ?> member) {
        return member.getAnnotationConsideringFieldAndGetterIfSupported(TunableField.class);
    }
}
