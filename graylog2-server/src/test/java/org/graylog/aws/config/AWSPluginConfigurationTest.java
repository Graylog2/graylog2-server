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
package org.graylog.aws.config;

import com.amazonaws.regions.Regions;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.graylog.aws.config.AWSPluginConfiguration.createDefault;

public class AWSPluginConfigurationTest {
    @Test
    public void lookupRegions() throws Exception {
        final AWSPluginConfiguration config = createDefault()
                .toBuilder()
                .lookupRegions("us-west-1,eu-west-1 ,  us-east-1 ")
                .build();

        assertThat(config.getLookupRegions()).containsExactly(Regions.US_WEST_1, Regions.EU_WEST_1, Regions.US_EAST_1);
    }

    @Test
    public void lookupRegionsWithEmptyValue() throws Exception {
        final AWSPluginConfiguration config = createDefault()
                .toBuilder()
                .lookupRegions("")
                .build();

        assertThat(config.getLookupRegions()).isEmpty();
    }

    @Test
    public void encryptsSecretKeyAndGeneratesSalt() {
        final AWSPluginConfiguration config = createDefault().toBuilder()
                .secretKey("verysecret", "myencryptionKey!")
                .build();

        assertThat(config.encryptedSecretKey()).isNotEqualTo("verysecret");
        assertThat(config.secretKeySalt()).isNotBlank();
        assertThat(config.secretKey("myencryptionKey!")).isEqualTo("verysecret");
    }

    @Test
    public void builderDoesNotTamperWithEncryptedSecretKey() {
        final AWSPluginConfiguration config = createDefault().toBuilder()
                .secretKey("verysecret", "myencryptionKey!")
                .build();

        final AWSPluginConfiguration newConfig = config.toBuilder()
                .accessKey("somethingElse")
                .build();

        assertThat(newConfig.secretKey("myencryptionKey!")).isEqualTo("verysecret");
    }

    @Test
    public void returnsEmptyStringIfEncryptedSecretKeyIsNotSet() {
        assertThat(createDefault().secretKey("foo")).isNull();
    }
}
