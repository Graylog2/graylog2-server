package org.graylog.failure;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;


public class DefaultFailureHandlingConfigurationTest {

    private final DefaultFailureHandlingConfiguration underTest = new DefaultFailureHandlingConfiguration();

    @Test
    public void defaultValuesNotChanged() {
        assertThat(underTest.keepFailedMessageDuplicate()).isTrue();
        assertThat(underTest.submitProcessingFailures()).isFalse();
    }

}
