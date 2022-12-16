package org.graylog2.streams.input;

import org.graylog2.inputs.Input;
import org.graylog2.inputs.InputService;
import org.graylog2.shared.inputs.InputDescription;
import org.graylog2.shared.inputs.MessageInputFactory;

import javax.inject.Inject;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class StreamRuleServerInputsProvider implements StreamRuleInputsProvider {

    private final InputService inputService;
    private final Map<String, InputDescription> inputDescriptions;

    @Inject
    public StreamRuleServerInputsProvider(InputService inputService, MessageInputFactory messageInputFactory) {
        this.inputService = inputService;
        inputDescriptions = messageInputFactory.getAvailableInputs();
    }

    @Override
    public Set<StreamRuleInput> inputs() {
        return inputService.all().stream()
                .map(this::toStreamRuleInput)
                .collect(Collectors.toSet());
    }

    private StreamRuleInput toStreamRuleInput(Input input) {
        return StreamRuleInput.builder()
                .title(input.getTitle())
                .name(getInputName(input))
                .id(input.getId())
                .build();
    }

    private String getInputName(Input input) {
        return InputDescription.getInputDescriptionName(inputDescriptions.get(input.getType()), input.getType());
    }

}
