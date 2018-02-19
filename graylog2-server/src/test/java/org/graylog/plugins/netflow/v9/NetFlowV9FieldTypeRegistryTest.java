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
package org.graylog.plugins.netflow.v9;

import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class NetFlowV9FieldTypeRegistryTest {
    @Test
    public void getReturnsExistingFieldType() throws Exception {
        final NetFlowV9FieldTypeRegistry typeRegistry = NetFlowV9FieldTypeRegistry.create();
        final NetFlowV9FieldType fieldType = typeRegistry.get(1);
        assertThat(fieldType.id()).isEqualTo(1);
        assertThat(fieldType.name()).isEqualTo("in_bytes");
        assertThat(fieldType.valueType()).isEqualTo(NetFlowV9FieldType.ValueType.UINT32);
    }

    @Test
    public void getReturnsNullForMissingFieldType() throws Exception {
        final NetFlowV9FieldTypeRegistry typeRegistry = NetFlowV9FieldTypeRegistry.create();
        final NetFlowV9FieldType fieldType = typeRegistry.get(123456);
        assertThat(fieldType).isNull();
    }

    @Test
    public void asMap() throws Exception {
        final NetFlowV9FieldTypeRegistry typeRegistry = NetFlowV9FieldTypeRegistry.create();
        final Map<Integer, NetFlowV9FieldType> map = typeRegistry.asMap();
        assertThat(map)
                .isNotEmpty()
                .containsEntry(1, NetFlowV9FieldType.create(1, NetFlowV9FieldType.ValueType.UINT32, "in_bytes"));
    }

}