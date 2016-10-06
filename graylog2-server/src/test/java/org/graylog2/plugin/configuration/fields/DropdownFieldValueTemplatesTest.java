package org.graylog2.plugin.configuration.fields;

import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class DropdownFieldValueTemplatesTest {
    private enum TestEnum {
        ONE, TWO
    }

    @Test
    public void testBuildEnumMap() throws Exception {
        final Map<String, String> enumMap = DropdownField.ValueTemplates.valueMapFromEnum(TestEnum.class, (t) -> t.name().toLowerCase());
        assertThat(enumMap)
            .containsEntry("ONE", "one")
            .containsEntry("TWO", "two");
    }
}
