package org.graylog.events.search;

import com.google.common.collect.ImmutableSet;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MoreSearchTest {
    @Test
    public void buildStreamFilter() {
        final String filter1 = MoreSearch.buildStreamFilter(ImmutableSet.of("stream-1", "stream-2", "stream-3"));
        final String filter2 = MoreSearch.buildStreamFilter(ImmutableSet.of(" stream-1 ", "stream-2"));
        final String filter3 = MoreSearch.buildStreamFilter(ImmutableSet.of("stream-1"));

        assertThat(filter1).isEqualTo("streams:stream-1 OR streams:stream-2 OR streams:stream-3");
        assertThat(filter2).isEqualTo("streams:stream-1 OR streams:stream-2");
        assertThat(filter3).isEqualTo("streams:stream-1");

        assertThatThrownBy(() -> MoreSearch.buildStreamFilter(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be null");
        assertThatThrownBy(() -> MoreSearch.buildStreamFilter(ImmutableSet.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be empty");
    }
}