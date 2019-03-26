package org.graylog2.grok;

import org.junit.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class GrokPatternServiceTest {
    @Test
    public void extractPatternNames() {
        final Set<String> names = GrokPatternService.extractPatternNames("%{EMAILLOCALPART}@%{HOSTNAME}");

        assertThat(names).containsOnly("HOSTNAME", "EMAILLOCALPART");
    }
}