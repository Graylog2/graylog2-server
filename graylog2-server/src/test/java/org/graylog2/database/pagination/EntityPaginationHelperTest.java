package org.graylog2.database.pagination;

import org.graylog.grn.GRNDescriptor;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class EntityPaginationHelperTest {

    static Stream<org.junit.jupiter.params.provider.Arguments> filterProvider() {
        return Stream.of(
                org.junit.jupiter.params.provider.Arguments.of("type:example", true),
                org.junit.jupiter.params.provider.Arguments.of("title:example", true),
                org.junit.jupiter.params.provider.Arguments.of("example", true),
                org.junit.jupiter.params.provider.Arguments.of("invalid:filter", false),
                org.junit.jupiter.params.provider.Arguments.of(null, true)
        );
    }

    @ParameterizedTest
    @MethodSource("filterProvider")
    void testBuildPredicateParameterized(String filter, boolean expected) {
        Predicate<GRNDescriptor> predicate = EntityPaginationHelper.buildPredicate(
                filter,
                descriptor -> "example",
                descriptor -> "Example Title"
        );

        assertThat(predicate.test(mock(GRNDescriptor.class))).isEqualTo(expected);
    }
}
