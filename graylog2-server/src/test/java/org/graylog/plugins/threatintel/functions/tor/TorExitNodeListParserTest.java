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
package org.graylog.plugins.threatintel.functions.tor;

import com.google.common.collect.Lists;
import org.graylog.plugins.threatintel.adapters.tor.TorExitNodeListParser;
import org.junit.Before;
import org.junit.Test;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class TorExitNodeListParserTest {
    private TorExitNodeListParser parser;

    @Before
    public void setUp() throws Exception {
        this.parser = new TorExitNodeListParser();
    }

    @Test
    public void parseValidExitNodeList() throws Exception {
        final URL torExitNodeListURL = getClass().getResource("TorExitNodeList-20170814133408.txt");
        final Path torExitNodeListPath = Paths.get(torExitNodeListURL.toURI());
        final String torExitNodeList = new String(Files.readAllBytes(torExitNodeListPath), StandardCharsets.UTF_8);

        final Map<String, List<String>> result = parser.parse(torExitNodeList);

        assertThat(result)
                .isNotNull()
                .isNotEmpty()
                .hasSize(873)
                .contains(new AbstractMap.SimpleEntry<String, List<String>>("51.15.79.107", Lists.newArrayList("5D5006E4992F2F97DF4F8B926C3688870EB52BD8")))
                .contains(new AbstractMap.SimpleEntry<String, List<String>>("104.223.123.98", Lists.newArrayList(
                        "02A627FA195809A3ABE031B7864CCA7A310F1D44",
                        "7016E939A2DD6EF2FB66A33F1DD45357458B737F",
                        "8175A86D8896CEA37FDC67311F9BDC1DDCBE8136",
                        "D4010FAD096CFB59278015F711776D8CCB2735EC"
                )))
                .doesNotContainKey("1.2.3.4");
    }

    @Test
    public void parseNullList() throws Exception {
        final Map<String, List<String>> result = parser.parse(null);

        assertThat(result)
                .isNotNull()
                .isEmpty();
    }

    @Test
    public void parseEmptyList() throws Exception {
        final Map<String, List<String>> result = parser.parse("");

        assertThat(result)
                .isNotNull()
                .isEmpty();
    }
}