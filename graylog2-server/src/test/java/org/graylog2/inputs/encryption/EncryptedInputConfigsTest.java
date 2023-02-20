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
package org.graylog2.inputs.encryption;

import org.graylog2.security.encryption.EncryptedValue;
import org.graylog2.security.encryption.EncryptedValueService;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class EncryptedInputConfigsTest {

    final EncryptedValueService encryptedValueService = new EncryptedValueService(UUID.randomUUID().toString());

    @Test
    void testMerge_replaceValue() {
        final Map<String, Object> orig = Map.of(
                "unencrypted", "old unencrypted",
                "encrypted", encryptedValueService.encrypt("old encrypted")
        );
        final Map<String, Object> update = Map.of(
                "unencrypted", "new unencrypted",
                "encrypted", encryptedValueService.encrypt("new encrypted")
        );

        assertThat(EncryptedInputConfigs.merge(orig, update)).isEqualTo(update);
    }

    @Test
    void testMerge_keepValue() {
        final Map<String, Object> orig = Map.of(
                "unencrypted", "old unencrypted",
                "encrypted", encryptedValueService.encrypt("old encrypted")
        );
        final Map<String, Object> update = Map.of(
                "encrypted", EncryptedValue.createWithKeepValue()
        );

        assertThat(EncryptedInputConfigs.merge(orig, update)).isEqualTo(orig);
    }

    @Test
    void testMerge_deleteValue() {
        final Map<String, Object> orig = Map.of(
                "unencrypted", "old unencrypted",
                "encrypted", encryptedValueService.encrypt("old encrypted")
        );

        // need maps that support null values
        final Map<String, Object> update = new HashMap<>();
        update.put("unencrypted", null);
        update.put("encrypted", EncryptedValue.createWithDeleteValue());

        final Map<String, Object> expected = new HashMap<>();
        expected.put("unencrypted", null);
        expected.put("encrypted", EncryptedValue.createUnset());

        assertThat(EncryptedInputConfigs.merge(orig, update)).isEqualTo(expected);
    }
}
