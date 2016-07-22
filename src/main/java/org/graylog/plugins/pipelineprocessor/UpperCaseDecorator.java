package org.graylog.plugins.pipelineprocessor;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMultimap;
import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.pipelineprocessor.ast.Pipeline;
import org.graylog.plugins.pipelineprocessor.ast.Rule;
import org.graylog.plugins.pipelineprocessor.parser.PipelineRuleParser;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.processors.listeners.NoopInterpreterListener;
import org.graylog2.decorators.Decorator;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.TextField;
import org.graylog2.plugin.decorators.SearchResponseDecorator;
import org.graylog2.rest.models.messages.responses.ResultMessageSummary;
import org.graylog2.rest.resources.search.responses.SearchResponse;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class UpperCaseDecorator implements SearchResponseDecorator {
    private static final String CK_FIELD_NAME = "fieldName";
    private static final String CK_PIPELINE_DEFINITION = "pipeline \"Uppercase decorator\"\nstage 0 match either\nrule \"Uppercase field\"\nend";

    private final List<Pipeline> pipelines;
    private final PipelineInterpreter pipelineInterpreter;
    private final Decorator decorator;

    public interface Factory extends SearchResponseDecorator.Factory {
        @Override
        UpperCaseDecorator create(Decorator decorator);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config implements SearchResponseDecorator.Config {
        @Inject
        public Config() {
        }

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            return new ConfigurationRequest() {{
                addField(new TextField(CK_FIELD_NAME, "Field Name", "", "The Name of the field which should be uppercased"));
            }};
        };
    }

    public static class Descriptor extends SearchResponseDecorator.Descriptor {
        public Descriptor() {
            super("Uppercase Decorator", "http://docs.graylog.org/en/2.0/pages/pipelines.html", "Uppercase Decorator");
        }
    }

    @Inject
    public UpperCaseDecorator(PipelineInterpreter pipelineInterpreter,
                              PipelineRuleParser pipelineRuleParser,
                              @Assisted Decorator decorator) {
        this.pipelineInterpreter = pipelineInterpreter;
        this.decorator = decorator;
        final String fieldName = (String)decorator.config().get(CK_FIELD_NAME);

        this.pipelines = pipelineRuleParser.parsePipelines(CK_PIPELINE_DEFINITION);
        final List<Rule> rules = ImmutableList.of(pipelineRuleParser.parseRule(getRuleForField(fieldName), true));
        this.pipelines.forEach(pipeline -> {
            pipeline.stages().forEach(stage -> stage.setRules(rules));
        });
    }

    @Override
    public SearchResponse apply(SearchResponse searchResponse) {
        final List<ResultMessageSummary> results = new ArrayList<>();
        searchResponse.messages().forEach((inMessage) -> {
            final Map<String, Object> originalMessage = ImmutableMap.copyOf(inMessage.message());
            final Message message = new Message(inMessage.message());
            final List<Message> additionalCreatedMessages = pipelineInterpreter.processForResolvedPipelines(message,
                    message.getId(),
                    new HashSet<>(this.pipelines),
                    new NoopInterpreterListener());

            results.add(ResultMessageSummary.create(inMessage.highlightRanges(), message.getFields(), inMessage.index()));
            additionalCreatedMessages.forEach((additionalMessage) -> {
                // TODO: pass proper highlight ranges. Need to rebuild them for new messages.
                results.add(ResultMessageSummary.create(
                        ImmutableMultimap.of(),
                        additionalMessage.getFields(),
                        "[created from decorator]"
                ));
            });
        });

        return searchResponse.toBuilder().messages(results).build();
    }

    private String getRuleForField(String fieldName) {
        return "rule \"Uppercase field\"\n" +
                "when\n" +
                "has_field(\"" + fieldName + "\")\n" +
                "then\n" +
                "set_field(\"" + fieldName + "\", uppercase(to_string($message." + fieldName + ")));\n" +
                "end";
    }
}
