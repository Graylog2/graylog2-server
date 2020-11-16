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
package org.graylog.events.fields.providers;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.floreysoft.jmte.Engine;
import com.floreysoft.jmte.template.VariableDescription;
import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableMap;
import com.google.inject.assistedinject.Assisted;
import org.apache.commons.lang3.StringUtils;
import org.graylog.events.event.EventWithContext;
import org.graylog.events.fields.FieldValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import java.util.Map;

import static com.google.common.base.Strings.isNullOrEmpty;

public class TemplateFieldValueProvider extends AbstractFieldValueProvider {
    public interface Factory extends AbstractFieldValueProvider.Factory<TemplateFieldValueProvider> {
        @Override
        TemplateFieldValueProvider create(FieldValueProvider.Config config);
    }

    private static final Logger LOG = LoggerFactory.getLogger(TemplateFieldValueProvider.class);
    private final Config config;
    private final Engine templateEngine;

    @Inject
    public TemplateFieldValueProvider(@Assisted FieldValueProvider.Config config, Engine templateEngine) {
        super(config);
        this.config = (Config) config;
        this.templateEngine = templateEngine;
    }

    @Override
    protected FieldValue doGet(String fieldName, EventWithContext eventWithContext) {
        final ImmutableMap.Builder<String, Object> dataModelBuilder = ImmutableMap.builder();

        if (eventWithContext.messageContext().isPresent()) {
            dataModelBuilder.put("source", eventWithContext.messageContext().get().getFields());
        } else if (eventWithContext.eventContext().isPresent()) {
            dataModelBuilder.put("source", eventWithContext.eventContext().get().toDto().fields());
        }

        final ImmutableMap<String, Object> dataModel = dataModelBuilder.build();

        if (!isValidTemplate(config.template(), dataModel)) {
            return FieldValue.error();
        }

        try {
            return FieldValue.string(templateEngine.transform(config.template(), dataModel));
        } catch (Exception e) {
            LOG.error("Couldn't render field template \"{}\"", config.template(), e);
            return FieldValue.error();
        }
    }

    private boolean isValidTemplate(String template, Map<String, Object> dataModel) {
        // Check if we have a syntax error in the template. (e.g. "hello ${source.foo" - missing "}")
        if (template.contains(templateEngine.getExprStartToken())) {
            final int startTokenCount = StringUtils.countMatches(template, templateEngine.getExprStartToken());
            final int endTokenCount = StringUtils.countMatches(template, templateEngine.getExprEndToken());

            if (startTokenCount != endTokenCount) {
                LOG.error("Syntax error in template \"{}\" - uneven number of start and end tokens (\"${\" and \"}\")", template);
                return false;
            }
        }

        // If the user wants all variables to have values, we need to check the template variables against the data model
        if (config.requireValues()) {
            boolean error = false;

            for (VariableDescription variable : templateEngine.getUsedVariableDescriptions(template)) {
                final String tmpl = String.join("", templateEngine.getExprStartToken(), variable.name, templateEngine.getExprEndToken());

                // Probably not the most efficient way to check the variable, but it works
                final String result = templateEngine.transform(tmpl, dataModel);

                // If there are any variables used in the template, there should be a value for each of them
                if (variable.context == VariableDescription.Context.TEXT && isNullOrEmpty(result)) {
                    LOG.error("No value found for variable \"{}\" in template \"{}\"", variable.name, template);
                    error = true;
                }
            }

            if (error) {
                return false;
            }
        }

        return true;
    }

    @AutoValue
    @JsonTypeName(Config.TYPE_NAME)
    @JsonDeserialize(builder = Config.Builder.class)
    public static abstract class Config implements AbstractFieldValueProvider.Config {
        public static final String TYPE_NAME = "template-v1";

        private static final String FIELD_TEMPLATE = "template";
        private static final String FIELD_REQUIRE_VALUES = "require_values";

        @JsonProperty(FIELD_TEMPLATE)
        public abstract String template();

        @JsonProperty(FIELD_REQUIRE_VALUES)
        public abstract boolean requireValues();

        public static Builder builder() {
            return Builder.create();
        }

        public abstract Builder toBuilder();

        @AutoValue.Builder
        public static abstract class Builder implements FieldValueProvider.Config.Builder<Builder> {
            @JsonCreator
            public static Builder create() {
                return new AutoValue_TemplateFieldValueProvider_Config.Builder()
                        .type(TYPE_NAME)
                        .requireValues(false);
            }

            @JsonProperty(FIELD_TEMPLATE)
            public abstract Builder template(String template);

            @JsonProperty(FIELD_REQUIRE_VALUES)
            public abstract Builder requireValues(boolean requireValues);

            public abstract Config build();
        }
    }
}
