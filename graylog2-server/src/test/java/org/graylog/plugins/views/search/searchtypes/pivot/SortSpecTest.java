/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.plugins.views.search.searchtypes.pivot;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SortSpecTest {

    @Test
    void directionDeserialize() {
        Assertions.assertThat(SortSpec.Direction.deserialize(null)).isNull();
        Assertions.assertThat(SortSpec.Direction.deserialize("")).isNull();
        Assertions.assertThat(SortSpec.Direction.deserialize("  ")).isNull();

        Assertions.assertThatThrownBy(() -> SortSpec.Direction.deserialize("blah"))
                        .isInstanceOf(IllegalArgumentException.class)
                        .hasMessageContaining("Failed to parse sort direction");

        Assertions.assertThat(SortSpec.Direction.deserialize("asc")).isEqualTo(SortSpec.Direction.Ascending);
        Assertions.assertThat(SortSpec.Direction.deserialize("ascending")).isEqualTo(SortSpec.Direction.Ascending);
        Assertions.assertThat(SortSpec.Direction.deserialize("Ascending")).isEqualTo(SortSpec.Direction.Ascending);
        Assertions.assertThat(SortSpec.Direction.deserialize(" AscEnding \n")).isEqualTo(SortSpec.Direction.Ascending);

        Assertions.assertThat(SortSpec.Direction.deserialize("desc")).isEqualTo(SortSpec.Direction.Descending);
        Assertions.assertThat(SortSpec.Direction.deserialize("descending")).isEqualTo(SortSpec.Direction.Descending);
    }
}
