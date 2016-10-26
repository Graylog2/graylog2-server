package org.graylog.plugins.pipelineprocessor.codegen;

import com.google.common.collect.Maps;

import org.graylog.plugins.pipelineprocessor.BaseParserTest;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.messages.HasField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetFields;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexMatch;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class Codegen extends BaseParserTest {
    private static final Logger log = LoggerFactory.getLogger(Codegen.class);

    @BeforeClass
    public static void registerFunctions() {

        Map<String, Function<?>> functions = Maps.newHashMap();

        functions.put(StringConversion.NAME, new StringConversion());
        functions.put(SetField.NAME, new SetField());
        functions.put(SetFields.NAME, new SetFields());
        functions.put(HasField.NAME, new HasField());
        functions.put(RegexMatch.NAME, new RegexMatch());

        functionRegistry = new FunctionRegistry(functions);
    }

    @Test
    public void runCodegen() {
        final Rule rule = parser.parseRule(ruleForTest(), true);

        log.info("Code:\n{}", CodeGenerator.codeForRule(rule));
    }

}
