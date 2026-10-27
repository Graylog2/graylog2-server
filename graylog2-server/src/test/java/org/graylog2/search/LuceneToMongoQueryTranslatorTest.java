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
package org.graylog2.search;

import com.mongodb.MongoClientSettings;
import com.mongodb.client.model.Filters;
import org.bson.BsonDocument;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.conversions.Bson;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LuceneToMongoQueryTranslatorTest {
    private static final CodecRegistry CODEC_REGISTRY = MongoClientSettings.getDefaultCodecRegistry();

    private final LuceneToMongoQueryTranslator translator = new LuceneToMongoQueryTranslator("message", Map.of(
            "actor", SearchQueryField.create("actor"),
            "action", SearchQueryField.create("action"),
            "status", SearchQueryField.create("http_status", SearchQueryField.Type.INT),
            "timestamp", SearchQueryField.create("timestamp", SearchQueryField.Type.DATE),
            "tags", SearchQueryField.create("tags", SearchQueryField.Type.STRING,
                    (field, value) -> Filters.elemMatch(field, Filters.eq("name", value.getValue())))
    ));

    private String translate(String query) {
        return toJson(translator.translate(query));
    }

    private static String toJson(Bson bson) {
        return bson.toBsonDocument(BsonDocument.class, CODEC_REGISTRY).toJson();
    }

    private static String json(String json) {
        return BsonDocument.parse(json).toJson();
    }

    private static String contains(String field, String text) {
        return "{\"" + field + "\": {\"$regularExpression\": {\"pattern\": \"\\\\Q" + text + "\\\\E\", \"options\": \"i\"}}}";
    }

    @Test
    void matchesAllForEmptyQuery() {
        assertThat(translate(null)).isEqualTo(json("{}"));
        assertThat(translate("")).isEqualTo(json("{}"));
        assertThat(translate(" * ")).isEqualTo(json("{}"));
        assertThat(translate("*:*")).isEqualTo(json("{}"));
    }

    @Test
    void searchesTermsInDefaultField() {
        assertThat(translate("login")).isEqualTo(json(contains("message", "login")));
        assertThat(translate("\"failed login\"")).isEqualTo(json(contains("message", "failed login")));
    }

    @Test
    void searchesTextInStringFields() {
        assertThat(translate("actor:jane")).isEqualTo(json(contains("actor", "jane")));
        assertThat(translate("actor:\"jane doe\"")).isEqualTo(json(contains("actor", "jane doe")));
        assertThat(translate("actor:a.b\\:c")).isEqualTo(json(contains("actor", "a.b:c")));
    }

    @Test
    void searchesValuesInOtherFields() {
        assertThat(translate("status:403")).isEqualTo(json("{\"http_status\": 403}"));
    }

    @Test
    void comparesValues() {
        assertThat(translate("status:>=400")).isEqualTo(json("{\"http_status\": {\"$gte\": 400}}"));
        assertThat(translate("status:>400")).isEqualTo(json("{\"http_status\": {\"$gt\": 400}}"));
        assertThat(translate("status:<=400")).isEqualTo(json("{\"http_status\": {\"$lte\": 400}}"));
        assertThat(translate("status:<400")).isEqualTo(json("{\"http_status\": {\"$lt\": 400}}"));
    }

    @Test
    void searchesRanges() {
        assertThat(translate("status:[400 TO 499]"))
                .isEqualTo(json("{\"$and\": [{\"http_status\": {\"$gte\": 400}}, {\"http_status\": {\"$lte\": 499}}]}"));
        assertThat(translate("status:{400 TO 499}"))
                .isEqualTo(json("{\"$and\": [{\"http_status\": {\"$gt\": 400}}, {\"http_status\": {\"$lt\": 499}}]}"));
        assertThat(translate("status:[500 TO *]")).isEqualTo(json("{\"http_status\": {\"$gte\": 500}}"));
        assertThat(translate("actor:{a TO m]"))
                .isEqualTo(json("{\"$and\": [{\"actor\": {\"$gt\": \"a\"}}, {\"actor\": {\"$lte\": \"m\"}}]}"));
    }

    @Test
    void convertsDates() {
        assertThat(translator.translate("timestamp:[\"2026-01-01T00:00:00.000Z\" TO *]"))
                .isEqualTo(Filters.gte("timestamp", new DateTime(2026, 1, 1, 0, 0, DateTimeZone.UTC)));
    }

    @Test
    void searchesWildcards() {
        assertThat(translate("actor:ja?e*"))
                .isEqualTo(json("{\"actor\": {\"$regularExpression\": {\"pattern\": \"^\\\\Qja\\\\E.\\\\Qe\\\\E.*$\", \"options\": \"i\"}}}"));
        assertThat(translate("actor:jan*"))
                .isEqualTo(json("{\"actor\": {\"$regularExpression\": {\"pattern\": \"^\\\\Qjan\\\\E.*$\", \"options\": \"i\"}}}"));
        assertThat(translate("actor:*.doe"))
                .isEqualTo(json("{\"actor\": {\"$regularExpression\": {\"pattern\": \"^.*\\\\Q.doe\\\\E$\", \"options\": \"i\"}}}"));
    }

    @Test
    void searchesExistingFields() {
        assertThat(translate("_exists_:actor")).isEqualTo(json("{\"actor\": {\"$ne\": null}}"));
        assertThat(translate("actor:*")).isEqualTo(json("{\"actor\": {\"$ne\": null}}"));
    }

    @Test
    void combinesConditions() {
        assertThat(translate("actor:jane AND action:create"))
                .isEqualTo(json("{\"$and\": [" + contains("actor", "jane") + ", " + contains("action", "create") + "]}"));
        assertThat(translate("actor:jane OR action:create"))
                .isEqualTo(json("{\"$or\": [" + contains("actor", "jane") + ", " + contains("action", "create") + "]}"));
        assertThat(translate("actor:jane action:create"))
                .isEqualTo(json("{\"$or\": [" + contains("actor", "jane") + ", " + contains("action", "create") + "]}"));
        assertThat(translate("actor:jane AND (action:create OR action:delete)"))
                .isEqualTo(json("{\"$and\": [" + contains("actor", "jane") + ", {\"$or\": [" + contains("action", "create") + ", " + contains("action", "delete") + "]}]}"));
    }

    @Test
    void negatesConditions() {
        assertThat(translate("NOT actor:jane")).isEqualTo(json("{\"$nor\": [" + contains("actor", "jane") + "]}"));
        assertThat(translate("-actor:jane")).isEqualTo(json("{\"$nor\": [" + contains("actor", "jane") + "]}"));
        assertThat(translate("action:create AND NOT actor:jane"))
                .isEqualTo(json("{\"$and\": [" + contains("action", "create") + ", {\"$nor\": [" + contains("actor", "jane") + "]}]}"));
    }

    @Test
    void ignoresOptionalConditionsNextToRequiredOnes() {
        assertThat(translate("+action:create actor:jane")).isEqualTo(json(contains("action", "create")));
    }

    @Test
    void ignoresBoosts() {
        assertThat(translate("actor:jane^2")).isEqualTo(json(contains("actor", "jane")));
    }

    @Test
    void usesFilterCreatorOfField() {
        assertThat(translate("tags:important")).isEqualTo(json("{\"tags\": {\"$elemMatch\": {\"name\": \"important\"}}}"));
    }

    @Test
    void rejectsUnknownFields() {
        assertThatThrownBy(() -> translator.translate("password:secret"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown field <password>")
                .hasMessageContaining("action, actor, status, tags, timestamp");
        assertThatThrownBy(() -> translator.translate("_exists_:password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown field <password>");
    }

    @Test
    void rejectsInvalidQueries() {
        assertThatThrownBy(() -> translator.translate("actor:jane\nAND )"))
                .isInstanceOfSatisfying(InvalidLuceneQueryException.class, e -> {
                    assertThat(e.position()).contains(new InvalidLuceneQueryException.Position(2, 4, 2, 5));
                    assertThat(e).hasMessage("Invalid query: unexpected <)> at line 2, column 5.");
                });
        assertThatThrownBy(() -> translator.translate("actor:(jane"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageStartingWith("Invalid query:");
        assertThatThrownBy(() -> translator.translate("status:abc"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid value <abc> for field <http_status>");
        assertThatThrownBy(() -> translator.translate("status:4*"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Wildcards are only supported for text fields");
    }

    @Test
    void reportsTheReasonAndPositionOfErrors() {
        assertThatThrownBy(() -> translator.translate("actor:jane AND )"))
                .isInstanceOfSatisfying(InvalidLuceneQueryException.class, e -> {
                    assertThat(e.reason()).isEqualTo(InvalidLuceneQueryException.Reason.PARSE_ERROR);
                    assertThat(e.position()).contains(new InvalidLuceneQueryException.Position(1, 15, 1, 16));
                    assertThat(e).hasMessage("Invalid query: unexpected <)> at line 1, column 16.");
                });
        assertThatThrownBy(() -> translator.translate("actor:jane\nAND )"))
                .isInstanceOfSatisfying(InvalidLuceneQueryException.class, e -> {
                    assertThat(e.position()).contains(new InvalidLuceneQueryException.Position(2, 4, 2, 5));
                    assertThat(e).hasMessage("Invalid query: unexpected <)> at line 2, column 5.");
                });
        assertThatThrownBy(() -> translator.translate("actor:(jane"))
                .isInstanceOfSatisfying(InvalidLuceneQueryException.class, e ->
                        assertThat(e).hasMessage("Invalid query: the query ended unexpectedly."));
        assertThatThrownBy(() -> translator.translate("password:secret"))
                .isInstanceOfSatisfying(InvalidLuceneQueryException.class, e -> {
                    assertThat(e.reason()).isEqualTo(InvalidLuceneQueryException.Reason.UNKNOWN_FIELD);
                    assertThat(e.position()).isEmpty();
                });
        assertThatThrownBy(() -> translator.translate("status:abc"))
                .isInstanceOfSatisfying(InvalidLuceneQueryException.class, e ->
                        assertThat(e.reason()).isEqualTo(InvalidLuceneQueryException.Reason.INVALID_VALUE));
        assertThatThrownBy(() -> translator.translate("actor:jane~"))
                .isInstanceOfSatisfying(InvalidLuceneQueryException.class, e ->
                        assertThat(e.reason()).isEqualTo(InvalidLuceneQueryException.Reason.UNSUPPORTED_QUERY));
    }

    @Test
    void rejectsUnsupportedQueries() {
        assertThatThrownBy(() -> translator.translate("actor:jane~"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported");
        assertThatThrownBy(() -> translator.translate("actor:/ja.e/"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("not supported");
        assertThatThrownBy(() -> translator.translate("\"failed login\"~2"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("proximity queries are not supported");
    }
}
