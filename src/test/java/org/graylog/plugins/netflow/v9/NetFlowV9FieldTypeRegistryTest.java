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