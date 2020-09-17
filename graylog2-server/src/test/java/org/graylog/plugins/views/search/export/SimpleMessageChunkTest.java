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
package org.graylog.plugins.views.search.export;

import org.assertj.core.data.Index;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.plugins.views.search.export.LinkedHashSetUtil.linkedHashSetOf;
import static org.graylog.plugins.views.search.export.TestData.simpleMessage;
import static org.graylog.plugins.views.search.export.TestData.simpleMessageChunk;

class SimpleMessageChunkTest {
    @Test
    void getsValuesInOrder() {
        Object[] msg1Values = {"2015-01-01 01:00:00.000", "source-1"};
        Object[] msg2Values = {"2015-01-02 01:00:00.000", "source-2"};
        SimpleMessageChunk sut = simpleMessageChunk("timestamp,source",
                msg1Values,
                msg2Values
        );

        assertThat(sut.getAllValuesInOrder()).contains(msg1Values, Index.atIndex(0)).contains(msg2Values, Index.atIndex(1));
    }

    @Test
    void valuesInOrderContainsMissingFieldsAsNull() {
        SimpleMessage msg1 = simpleMessage("timestamp,source", new Object[]{"2015-01-01 01:00:00.000", "source-1"});
        SimpleMessage msg2 = simpleMessage("timestamp", new Object[]{"2015-01-02 01:00:00.000"});

        SimpleMessageChunk sut = SimpleMessageChunk.from(
                linkedHashSetOf("timestamp", "source"),
                msg1, msg2);

        assertThat(sut.getAllValuesInOrder())
                .contains(new Object[]{"2015-01-01 01:00:00.000", "source-1"}, Index.atIndex(0))
                .contains(new Object[]{"2015-01-02 01:00:00.000", null}, Index.atIndex(1));
    }
}
