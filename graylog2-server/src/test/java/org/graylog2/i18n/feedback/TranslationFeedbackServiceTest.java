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
package org.graylog2.i18n.feedback;

import org.bson.Document;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.bindings.providers.MongoJackObjectMapperProvider;
import org.graylog2.database.MongoCollections;
import org.graylog2.database.PaginatedList;
import org.graylog2.database.filtering.DbQueryCreator;
import org.graylog2.rest.models.SortOrder;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MongoDBExtension.class)
class TranslationFeedbackServiceTest {
    private TranslationFeedbackService service;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        final MongoJackObjectMapperProvider objectMapperProvider = new MongoJackObjectMapperProvider(new ObjectMapperProvider().get());
        service = new TranslationFeedbackService(new MongoCollections(objectMapperProvider, mongodb.mongoConnection()));
    }

    private static CreateTranslationFeedbackRequest request(String language, String namespace, String key, String text) {
        return new CreateTranslationFeedbackRequest(language, namespace, key, text, "English " + text, "Besser", " ",
                "wrong", "/search", "nav > a");
    }

    @Test
    void createsReportsWithReporterAndOpenStatus() {
        final TranslationFeedbackDTO created = service.create(request("de", "search", "title", "Suche"), "proofreader");

        assertThat(created.id()).isNotNull();
        assertThat(created.reporter()).isEqualTo("proofreader");
        assertThat(created.status()).isEqualTo(TranslationFeedbackDTO.STATUS_OPEN);
        assertThat(created.comment()).as("blank values are not stored").isNull();
        assertThat(created.createdAt()).isNotNull();
        assertThat(service.get(created.id())).contains(created);
    }

    @Test
    void filtersPaginatedResults() {
        service.create(request("de", "search", "title", "Suche"), "a");
        service.create(request("fr", "search", "title", "Recherche"), "b");

        final DbQueryCreator queryCreator = new DbQueryCreator(TranslationFeedbackDTO.FIELD_TEXT, TranslationFeedbackResource.ATTRIBUTES);
        final PaginatedList<TranslationFeedbackDTO> result = service.findPaginated(
                queryCreator.createDbQuery(List.of("language:fr"), ""), "created_at", SortOrder.DESCENDING, 1, 50);

        assertThat(result).extracting(TranslationFeedbackDTO::text).containsExactly("Recherche");
    }

    @Test
    void updatesStatusAndDeletes() {
        final TranslationFeedbackDTO created = service.create(request("de", "search", "title", "Suche"), "a");

        assertThat(service.updateStatus(created.id(), TranslationFeedbackDTO.STATUS_FIXED))
                .hasValueSatisfying(updated -> assertThat(updated.status()).isEqualTo(TranslationFeedbackDTO.STATUS_FIXED));
        assertThat(service.delete(created.id())).isTrue();
        assertThat(service.get(created.id())).isEmpty();
        assertThat(service.updateStatus(created.id(), TranslationFeedbackDTO.STATUS_OPEN)).isEmpty();
    }

    @Test
    void exportsGroupedByLanguageNamespaceAndKey() {
        service.create(request("de", "search", "title", "Suche"), "a");
        service.create(request("de", "search", "title", "Suche"), "b");
        service.create(request("de", null, null, "Hardcoded text"), "a");

        final TranslationFeedbackExport export = TranslationFeedbackExport.of(service.findAll(new Document()),
                DateTime.now(DateTimeZone.UTC));

        assertThat(export.total()).isEqualTo(3);
        assertThat(export.languages().get("de").get("search").get("title")).hasSize(2);
        assertThat(export.languages().get("de").get(TranslationFeedbackExport.UNTRANSLATED).get("Hardcoded text")).hasSize(1);
    }

    @Test
    void exportsCsvWithEscaping() {
        service.create(new CreateTranslationFeedbackRequest("de", "search", "title", "Say \"hi\", please", null,
                "=SUM(A1)", null, "wrong", null, null), "a");

        final String[] lines = TranslationFeedbackExport.toCsv(service.findAll(new Document())).split("\r\n");

        assertThat(lines[0]).startsWith("id,language,namespace,key,status");
        assertThat(lines[1]).contains(",\"Say \"\"hi\"\", please\",").contains(",\"'=SUM(A1)\",");
    }
}
