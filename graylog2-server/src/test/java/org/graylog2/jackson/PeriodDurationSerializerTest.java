package org.graylog2.jackson;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.threeten.extra.PeriodDuration;

import java.util.Arrays;
import java.util.Collection;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

@RunWith(Parameterized.class)
public class PeriodDurationSerializerTest {
    private final ObjectMapper mapper = new ObjectMapperProvider().get();

    private final String input;
    private final String expectedSerialized;

    public PeriodDurationSerializerTest(String input, String expectedSerialized) {
        this.input = input;
        this.expectedSerialized = expectedSerialized;
    }

    @Parameterized.Parameters(name = "Input \"{0}\" serializes to \"{1}\"")
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][]{
                {"P1Y", "\"P1Y\""},
                {"PT12H", "\"PT12H\""},
                {"P1YT12H", "\"P1YT12H\""}
        });
    }

    @Test
    public void testSerialization() throws JsonProcessingException {
        final PeriodDuration instance = PeriodDuration.parse(input);
        final String serialized = mapper.writeValueAsString(instance);
        assertThat(serialized).isEqualTo(expectedSerialized);
    }
}
