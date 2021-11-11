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
package org.graylog.plugins.views.search.engine.validation;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

class ValidationMessageParserTest {
    @Test
    void testParseException() {
        final ValidationMessage message = ValidationMessageParser.getHumanReadableMessage("[graylog_0/7o7KffecQkuTEM7L_VGazA] QueryShardException[Failed to parse query [(lorem:ipsum OR dolor:sit AND _exists_:http_method]]; nested: ParseException[Cannot parse '(lorem:ipsum OR dolor:sit AND _exists_:http_method': Encountered \\\"<EOF>\\\" at line 1, column 50.\\nWas expecting one of:\\n    <AND> ...\\n    <OR> ...\\n    <NOT> ...\\n    \\\"+\\\" ...\\n    \\\"-\\\" ...\\n    <BAREOPER> ...\\n    \\\"(\\\" ...\\n    \\\")\\\" ...\\n    \\\"*\\\" ...\\n    \\\"^\\\" ...\\n    <QUOTED> ...\\n    <TERM> ...\\n    <FUZZY_SLOP> ...\\n    <PREFIXTERM> ...\\n    <WILDTERM> ...\\n    <REGEXPTERM> ...\\n    \\\"[\\\" ...\\n    \\\"{\\\" ...\\n    <NUMBER> ...\\n    ]; nested: ParseException[Encountered \\\"<EOF>\\\" at line 1, column 50.\\nWas expecting one of:\\n    <AND> ...\\n    <OR> ...\\n    <NOT> ...\\n    \\\"+\\\" ...\\n    \\\"-\\\" ...\\n    <BAREOPER> ...\\n    \\\"(\\\" ...\\n    \\\")\\\" ...\\n    \\\"*\\\" ...\\n    \\\"^\\\" ...\\n    <QUOTED> ...\\n    <TERM> ...\\n    <FUZZY_SLOP> ...\\n    <PREFIXTERM> ...\\n    <WILDTERM> ...\\n    <REGEXPTERM> ...\\n    \\\"[\\\" ...\\n    \\\"{\\\" ...\\n    <NUMBER> ...\\n    ];; org.apache.lucene.queryparser.classic.ParseException: Cannot parse '(lorem:ipsum OR dolor:sit AND _exists_:http_method': Encountered \\\"<EOF>\\\" at line 1, column 50.\\nWas expecting one of:\\n    <AND> ...\\n    <OR> ...\\n    <NOT> ...\\n    \\\"+\\\" ...\\n    \\\"-\\\" ...\\n    <BAREOPER> ...\\n    \\\"(\\\" ...\\n    \\\")\\\" ...\\n    \\\"*\\\" ...\\n    \\\"^\\\" ...\\n    <QUOTED> ...\\n    <TERM> ...\\n    <FUZZY_SLOP> ...\\n    <PREFIXTERM> ...\\n    <WILDTERM> ...\\n    <REGEXPTERM> ...\\n    \\\"[\\\" ...\\n    \\\"{\\\" ...\\n    <NUMBER> ...\\n   ");
        assertThat(message.errorType()).isEqualTo("QueryShardException");
        assertThat(message.errorMessage()).isEqualTo("Failed to parse query [(lorem:ipsum OR dolor:sit AND _exists_:http_method]");
    }

    @Test
    void testNFE() {
        final ValidationMessage message = ValidationMessageParser.getHumanReadableMessage("[graylog_0/Otu1lLawRTS9shE5LUzQiQ] QueryShardException[failed to create query: For input string: \\\"1,2\\\"]; nested: NumberFormatException[For input string: \\\"1,2\\\"];; java.lang.NumberFormatException: For input string: \\\"1,2\\");
        assertThat(message.errorType()).isEqualTo("QueryShardException");
        assertThat(message.errorMessage()).isEqualTo("failed to create query: For input string: \\\"1,2\\\"");

    }

    @Test
    void testWithoutIndexShard() {
        final ValidationMessage message = ValidationMessageParser.getHumanReadableMessage("QueryShardException[Failed to parse query [path:test/test]]; nested: ParseException[Cannot parse 'path:test/test': Lexical error at line 1, column 22. Encountered: <EOF> after : \\\"/test\\\"]; nested: TokenMgrError[Lexical error at line 1, column 22. Encountered: <EOF> after : \\\"/test\\\"];; org.apache.lucene.queryparser.classic.ParseException: Cannot parse 'path:test/test': Lexical error at line 1, column 22. Encountered: <EOF> after : \\\"/test\\\"\"");
        assertThat(message.errorType()).isEqualTo("QueryShardException");
        assertThat(message.errorMessage()).isEqualTo("Failed to parse query [path:test/test]");
        assertThat(message.line()).isEqualTo(1);
        assertThat(message.column()).isEqualTo(22);
    }

    @Test
    void testWithoutStructure() {
        // query is foo*:test
        final ValidationMessage error = ValidationMessageParser.getHumanReadableMessage("org.elasticsearch.index.query.QueryShardException: Can only use prefix queries on keyword, text and wildcard fields - not on [winlogbeat_event_code] which is of type [long]");
        assertThat(error.errorType()).isEqualTo("org.elasticsearch.index.query.QueryShardException");
        assertThat(error.errorMessage()).isEqualTo("Can only use prefix queries on keyword, text and wildcard fields - not on [winlogbeat_event_code] which is of type [long]");
    }
}
