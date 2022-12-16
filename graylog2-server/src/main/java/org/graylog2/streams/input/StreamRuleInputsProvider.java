package org.graylog2.streams.input;

import java.util.Set;

public interface StreamRuleInputsProvider {

    Set<StreamRuleInput> inputs();
}
