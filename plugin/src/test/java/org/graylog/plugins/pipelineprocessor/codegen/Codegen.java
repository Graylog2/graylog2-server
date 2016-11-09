package org.graylog.plugins.pipelineprocessor.codegen;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.google.common.io.Files;

import net.openhft.compiler.CachedCompiler;
import net.openhft.compiler.CompilerUtils;

import org.graylog.plugins.pipelineprocessor.BaseParserTest;
import org.graylog.plugins.pipelineprocessor.EvaluationContext;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.ast.functions.Function;
import org.graylog.plugins.pipelineprocessor.functions.conversion.StringConversion;
import org.graylog.plugins.pipelineprocessor.functions.messages.HasField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetField;
import org.graylog.plugins.pipelineprocessor.functions.messages.SetFields;
import org.graylog.plugins.pipelineprocessor.functions.strings.RegexMatch;
import org.graylog.plugins.pipelineprocessor.parser.FunctionRegistry;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.Tools;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.reflections.ReflectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@Ignore("Replaced by CodegenPipelineRuleParserTest")
public class Codegen extends BaseParserTest {
    private static final Logger log = LoggerFactory.getLogger(Codegen.class);
    private static final Path PARENT = Paths.get("/Users/kroepke/projects/graylog/graylog-project-repos/graylog-plugin-pipeline-processor/plugin/");
    public static final Path OUTFILE = Paths.get(PARENT.toString(), "src/main/java/org/graylog/plugins/pipelineprocessor/$dynamic/rules/rule$1.java");

    private static final CachedCompiler JCC = CompilerUtils.DEBUGGING ?
            new CachedCompiler(PARENT.resolve("src/test/java").toFile(), PARENT.resolve("target/compiled").toFile()) :
            CompilerUtils.CACHED_COMPILER;


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
    public void runCodegen() throws IOException {
        final Rule rule = parser.parseRule(ruleForTest(), true).withId("1");

        final String sourceCode = CodeGenerator.sourceCodeForRule(rule);
        Files.write(sourceCode,
                OUTFILE.toFile(),
                StandardCharsets.UTF_8);

        log.info("Code:\n{}", sourceCode);

        try {

            ClassLoader ruleClassloader = new ClassLoader() {};
            //noinspection unchecked
            Class<GeneratedRule> rule$1 = (Class<GeneratedRule>) JCC.loadFromJava(ruleClassloader, "org.graylog.plugins.pipelineprocessor.$dynamic.rules.rule$1", sourceCode);

            //noinspection unchecked
            final Set<Constructor> constructors = ReflectionUtils.getConstructors(rule$1, input -> input.getParameterCount() == 1);
            final Constructor onlyElement = Iterables.getOnlyElement(constructors);
            final GeneratedRule generatedRule = (GeneratedRule) onlyElement.newInstance(functionRegistry);

            final Message message = new Message("hello", "jenkins.torch.sh", Tools.nowUTC());
            message.addField("message", "#1234");
            message.addField("something_that_doesnt_exist", "foo");
            final EvaluationContext context = new EvaluationContext(message);

            final boolean when = generatedRule.when(context);
            if (when) {
                generatedRule.then(context);
            }
            log.info("created dynamic rule {} matches: {}", generatedRule.name(), when);

            assertThat(context.currentMessage().hasField("some_identifier")).isTrue();

        } catch (InvocationTargetException | ClassNotFoundException | InstantiationException | IllegalAccessException e) {
            log.error("Cannot load dynamically created class!", e);
        }
    }

}
