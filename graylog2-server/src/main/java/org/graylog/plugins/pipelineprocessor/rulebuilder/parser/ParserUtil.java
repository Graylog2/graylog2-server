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
package org.graylog.plugins.pipelineprocessor.rulebuilder.parser;

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateNotFoundException;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.graylog.plugins.pipelineprocessor.ast.functions.FunctionDescriptor;
import org.graylog.plugins.pipelineprocessor.ast.functions.ParameterDescriptor;
import org.graylog.plugins.pipelineprocessor.rulebuilder.RuleBuilderStep;
import org.graylog.plugins.pipelineprocessor.rulebuilder.db.RuleFragment;
import org.graylog2.bindings.providers.SecureFreemarkerConfigProvider;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static org.graylog2.shared.utilities.StringUtils.f;

public class ParserUtil {


    static final String generateForFunction(RuleBuilderStep step, FunctionDescriptor<?> function) {
        return generateForFunction(step, function, 1);
    }

    static final String generateForFunction(RuleBuilderStep step, FunctionDescriptor<?> function, int level) {
        String syntax = function.name() + "(";
        String params = function.params().stream()
                .map(p -> addFunctionParameter(p, step))
                .filter(Objects::nonNull)
                .collect(Collectors.joining("," + ConditionParser.NL));
        if (StringUtils.isEmpty(params)) {
            return syntax + ")";
        } else {
            return syntax + ConditionParser.NL + params + ConditionParser.NL + StringUtils.repeat("  ", level) + ")";
        }
    }

    static final String addFunctionParameter(ParameterDescriptor descriptor, RuleBuilderStep step) {
        final String parameterName = descriptor.name(); // parameter name needed by function
        final Map<String, Object> parameters = step.parameters();
        if (Objects.isNull(parameters)) {
            return null;
        }
        final Object value = parameters.get(parameterName); // parameter value set by rule definition
        String syntax = "    " + parameterName + " : ";
        if (value == null) {
            return null;
        } else if (value instanceof String valueString) {
            if (StringUtils.isEmpty(valueString)) {
                return null;
            } else if (valueString.startsWith("$")) { // value set as variable
                syntax += valueString.substring(1);
            } else {
                syntax += "\"" + StringEscapeUtils.escapeJava(valueString) + "\""; // value set as string
            }
        } else {
            syntax += value;
        }
        return syntax;
    }

    static final Configuration initializeFragmentTemplates(SecureFreemarkerConfigProvider secureFreemarkerConfigProvider, Map<String, RuleFragment> fragments) {
        final Configuration freemarkerConfiguration = secureFreemarkerConfigProvider.get();
        StringTemplateLoader stringTemplateLoader = new StringTemplateLoader();
        fragments.entrySet().stream().filter(c -> c.getValue().isFragment()).forEach(c -> stringTemplateLoader.putTemplate(c.getKey(), c.getValue().fragment()));
        freemarkerConfiguration.setTemplateLoader(stringTemplateLoader);
        return freemarkerConfiguration;
    }

    static final String generateForFragment(RuleBuilderStep step, Configuration configuration) {
        final String fragmentName = step.function();
        try {
            Template template = configuration.getTemplate(fragmentName);
            StringWriter writer = new StringWriter();
            Map<String, Object> filteredParams = new HashMap<>();
            if (step.parameters() != null) {
                for (Map.Entry<String, Object> val : step.parameters().entrySet()) {
                    if (val.getValue() instanceof String s) {
                        if (StringUtils.isBlank(s)) {
                        } else if (s.startsWith("$")) {
                            filteredParams.put(val.getKey(), s.substring(1));
                        } else {
                            filteredParams.put(val.getKey(), "\"" + s + "\"");
                        }
                    } else {
                        filteredParams.put(val.getKey(), val.getValue());
                    }
                }
            }
            template.process(filteredParams, writer);
            writer.close();
            return writer.toString();
        } catch (TemplateNotFoundException e) {
            throw new IllegalArgumentException(f("No template found for fragment %s", fragmentName));
        } catch (Exception e) {
            throw new IllegalArgumentException("Error converting fragment template to fragment.", e);
        }

    }

}
