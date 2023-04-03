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
package org.graylog2.inputs.codecs;

import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.Assertions;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CSVCodecTest {

    private static final String rawMessageContent = """
            Messort;Parameter;Zeitpunkt;HMW
            Salzburg Flughafen;Lufttemperatur [GradC];03.04.2023 00:30;3,0
            Salzburg Flughafen;Lufttemperatur [GradC];03.04.2023 01:00;3,0
            Salzburg Flughafen;Lufttemperatur [GradC];03.04.2023 01:30;3,1
            Salzburg Flughafen;Lufttemperatur [GradC];03.04.2023 02:00;3,1
            Salzburg Flughafen;Lufttemperatur [GradC];03.04.2023 02:30;3,0
            """;

    @Test
    void decodeMessages() {
        final Configuration codecConfig = new Configuration(Map.of(
                CSVCodec.CK_SOURCE, "salzburg opendata weather in CSV",
                CSVCodec.CK_DELIMITER, ";",
                CSVCodec.CK_URL, "https://www.salzburg.gv.at/ogd/bad388c1-e13f-484d-ba51-331a79537f5f/meteorologie-aktuell.csv"));

        final RawMessage rawMessage = new RawMessage(rawMessageContent.getBytes(StandardCharsets.UTF_8));
        final Collection<Message> messages = new CSVCodec(codecConfig).decodeMessages(rawMessage);

        Assertions.assertThat(messages)
                .hasSize(5)
                .allMatch(m -> m.getField("Messort").equals("Salzburg Flughafen"))
                .allMatch(m -> ((String)m.getField("Zeitpunkt")).startsWith("03.04.2023"));
    }
}
