package org.graylog.plugins.pipelineprocessor;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.inject.assistedinject.Assisted;
import org.graylog.plugins.pipelineprocessor.db.PipelineDao;
import org.graylog.plugins.pipelineprocessor.db.PipelineService;
import org.graylog.plugins.pipelineprocessor.processors.PipelineInterpreter;
import org.graylog.plugins.pipelineprocessor.processors.listeners.NoopInterpreterListener;
import org.graylog2.decorators.Decorator;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.ConfigurationRequest;
import org.graylog2.plugin.configuration.fields.ConfigurationField;
import org.graylog2.plugin.configuration.fields.DropdownField;
import org.graylog2.plugin.decorators.MessageDecorator;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class PipelineProcessorMessageDecorator implements MessageDecorator {
    private static final String CONFIG_FIELD_PIPELINE = "pipeline";

    private final PipelineInterpreter pipelineInterpreter;
    private final ImmutableSet<String> pipelines;

    public interface Factory extends MessageDecorator.Factory {
        @Override
        PipelineProcessorMessageDecorator create(Decorator decorator);

        @Override
        Config getConfig();

        @Override
        Descriptor getDescriptor();
    }

    public static class Config implements MessageDecorator.Config {
        private final PipelineService pipelineService;

        @Inject
        public Config(PipelineService pipelineService) {
            this.pipelineService = pipelineService;
        }

        @Override
        public ConfigurationRequest getRequestedConfiguration() {
            final Map<String, String> pipelineOptions = this.pipelineService.loadAll().stream()
                    .sorted((o1, o2) -> o1.title().compareTo(o2.title()))
                    .collect(Collectors.toMap(PipelineDao::id, PipelineDao::title));
            return new ConfigurationRequest() {{
                addField(new DropdownField(CONFIG_FIELD_PIPELINE,
                        "Pipeline",
                        "",
                        pipelineOptions,
                        "Which pipeline to use for message decoration",
                        ConfigurationField.Optional.NOT_OPTIONAL));
            }};
        };
    }

    public static class Descriptor extends MessageDecorator.Descriptor {
        public Descriptor() {
            super("Pipeline Processor Decorator", false, "http://docs.graylog.org/en/2.0/pages/pipelines.html", "Pipeline Processor Decorator");
        }
    }

    @Inject
    public PipelineProcessorMessageDecorator(PipelineInterpreter pipelineInterpreter,
                                             @Assisted Decorator decorator) {
        this.pipelineInterpreter = pipelineInterpreter;
        final String pipelineId = (String)decorator.config().get(CONFIG_FIELD_PIPELINE);
        if (Strings.isNullOrEmpty(pipelineId)) {
            this.pipelines = ImmutableSet.of();
        } else {
            this.pipelines = ImmutableSet.of(pipelineId);
        }
    }

    @Override
    public List<ResultMessage> apply(List<ResultMessage> resultMessages) {
        final List<ResultMessage> results = new ArrayList<>();
        if (pipelines.isEmpty()) {
            return resultMessages;
        }
        resultMessages.forEach((inMessage) -> {
            final Message message = inMessage.getMessage();
            final List<Message> additionalCreatedMessages = pipelineInterpreter.processForPipelines(message,
                    message.getId(),
                    pipelines,
                    new NoopInterpreterListener());
            final ResultMessage outMessage = ResultMessage.createFromMessage(message, inMessage.getIndex(), inMessage.getHighlightRanges());

            results.add(outMessage);
            additionalCreatedMessages.forEach((additionalMessage) -> {
                // TODO: pass proper highlight ranges. Need to rebuild them for new messages.
                results.add(ResultMessage.createFromMessage(additionalMessage, "[created from decorator]", ImmutableMultimap.of()));
            });
        });

        return results;
    }
}
