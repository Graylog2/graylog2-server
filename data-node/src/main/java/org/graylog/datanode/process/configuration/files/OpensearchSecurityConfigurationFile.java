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
package org.graylog.datanode.process.configuration.files;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import org.graylog2.security.JwtSecret;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;

public class OpensearchSecurityConfigurationFile implements DatanodeConfigFile {

    private static final ObjectMapper OBJECT_MAPPER = new YAMLMapper();
    private static final Path TARGET_PATH = Path.of("opensearch-security", "config.yml");
    private final JwtSecret signingKey;

    public OpensearchSecurityConfigurationFile(final JwtSecret signingKey) {
        this.signingKey = signingKey;
    }

    @Override
    public Path relativePath() {
        return TARGET_PATH;
    }

    @Override
    public void write(OutputStream stream) throws IOException {
        final InputStream configSource = getClass().getResourceAsStream("/opensearch/config/opensearch-security/config.yml");
        Map<String, Object> contents = OBJECT_MAPPER.readValue(configSource, new TypeReference<>() {});
        Map<String, Object> config = filterConfigurationMap(contents, "config", "dynamic", "authc", "jwt_auth_domain", "http_authenticator", "config");
        config.put("signing_key", signingKey.getBase64Encoded());
        OBJECT_MAPPER.writeValue(stream, contents);
    }


    private Map<String, Object> filterConfigurationMap(final Map<String, Object> map, final String... keys) {
        Map<String, Object> result = map;
        for (final String key : List.of(keys)) {
            result = (Map<String, Object>) result.get(key);
        }
        return result;
    }
}
