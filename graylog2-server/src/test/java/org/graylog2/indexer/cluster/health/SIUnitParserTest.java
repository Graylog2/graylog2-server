/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.indexer.cluster.health;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class SIUnitParserTest {
    @Test
    void returnsNullForUnparseableValue() {
        assertThat(SIUnitParser.parseBytesSizeValue("This is not a value")).isNull();
    }

    @DisplayName("Should parse SI Units properly and convert to bytes count")
    @ParameterizedTest(name = "should parse {0} as {1} bytes")
    @MethodSource("argumentsProvider")
    void parsesStrings(String siUnitValue, Long expected) {
        final ByteSize result = SIUnitParser.parseBytesSizeValue(siUnitValue);

        assertThat(result).isNotNull();
        assertThat(result.getBytes()).isEqualTo(expected);
    }

    private static Stream<Arguments> argumentsProvider() {
        return Stream.of(
                Arguments.of("0", 0L),
                Arguments.of("-1", -1L),
                Arguments.of("0b", 0L),
                Arguments.of("640b", 640L),
                Arguments.of("640k", 655360L),
                Arguments.of("640kb", 655360L),
                Arguments.of("12m", 12582912L),
                Arguments.of("2g", 2147483648L),
                Arguments.of("2G", 2147483648L),
                Arguments.of("2Gb", 2147483648L),
                Arguments.of("24t", 26388279066624L),
                Arguments.of("42p", 47287796087390208L)
        );
    }
}
