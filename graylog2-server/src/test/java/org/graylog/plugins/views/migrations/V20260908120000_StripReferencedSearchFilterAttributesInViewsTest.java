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
package org.graylog.plugins.views.migrations;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.graylog.testing.mongodb.MongoDBExtension;
import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBTestService;
import org.graylog2.plugin.cluster.ClusterConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MongoDBExtension.class)
@ExtendWith(MockitoExtension.class)
@MongoDBFixtures("V20260908120000_StripReferencedSearchFilterAttributesInViewsTest.json")
class V20260908120000_StripReferencedSearchFilterAttributesInViewsTest {

    @Mock
    private ClusterConfigService clusterConfigService;

    private MongoCollection<Document> viewsCollection;
    private V20260908120000_StripReferencedSearchFilterAttributesInViews migration;

    @BeforeEach
    void setUp(MongoDBTestService mongodb) {
        viewsCollection = mongodb.mongoCollection("views");
        migration = new V20260908120000_StripReferencedSearchFilterAttributesInViews(
                mongodb.mongoConnection(), clusterConfigService);
    }

    @Test
    void stripsExcessAttributesFromReferencedFilter() {
        migration.upgrade();

        final Document view = viewsCollection.find(Filters.eq("_id", new ObjectId("54e3deadbeefdeadbeef0001"))).first();
        assertThat(view).isNotNull();

        final Document filter = getFirstFilterOfFirstWidget(view);
        assertThat(filter.keySet()).containsExactlyInAnyOrder("type", "id");
        assertThat(filter.getString("type")).isEqualTo("referenced");
        assertThat(filter.getString("id")).isEqualTo("60e3deadbeefdeadbeef0001");
    }

    @Test
    void doesNotModifyInlineFilters() {
        migration.upgrade();

        final Document view = viewsCollection.find(Filters.eq("_id", new ObjectId("54e3deadbeefdeadbeef0002"))).first();
        assertThat(view).isNotNull();

        final Document filter = getFirstFilterOfFirstWidget(view);
        assertThat(filter.getString("type")).isEqualTo("inlineQueryString");
        assertThat(filter.keySet()).containsExactlyInAnyOrder("type", "id", "title", "description", "queryString", "negation", "disabled");
    }

    @Test
    void stripsReferencedFiltersAcrossMultipleStateEntriesAndKeepsInlineFilters() {
        migration.upgrade();

        final Document view = viewsCollection.find(Filters.eq("_id", new ObjectId("54e3deadbeefdeadbeef0003"))).first();
        assertThat(view).isNotNull();

        final Document state = view.get("state", Document.class);

        final List<Document> filters1 = getWidgets(state, "c01cfc3f-4b7e-4e5a-ad70-1234567890ef")
                .get(0).getList("filters", Document.class);
        assertThat(filters1.get(0).keySet()).containsExactlyInAnyOrder("type", "id");
        assertThat(filters1.get(0).getString("type")).isEqualTo("referenced");
        assertThat(filters1.get(0).getString("id")).isEqualTo("60e3deadbeefdeadbeef0002");
        assertThat(filters1.get(1).getString("type")).isEqualTo("inlineQueryString");
        assertThat(filters1.get(1).keySet()).containsExactlyInAnyOrder("type", "id", "title", "description", "queryString", "negation", "disabled");

        final List<Document> filters2 = getWidgets(state, "d11dfc3f-4b7e-4e5a-ad70-123456789012")
                .get(0).getList("filters", Document.class);
        assertThat(filters2.get(0).keySet()).containsExactlyInAnyOrder("type", "id");
        assertThat(filters2.get(0).getString("type")).isEqualTo("referenced");
        assertThat(filters2.get(0).getString("id")).isEqualTo("60e3deadbeefdeadbeef0003");
    }

    @Test
    void doesNotModifyAlreadyCleanReferencedFilter() {
        migration.upgrade();

        final Document view = viewsCollection.find(Filters.eq("_id", new ObjectId("54e3deadbeefdeadbeef0004"))).first();
        assertThat(view).isNotNull();

        final Document filter = getFirstFilterOfFirstWidget(view);
        assertThat(filter.getString("type")).isEqualTo("referenced");
        assertThat(filter.keySet()).containsExactlyInAnyOrder("type", "id");
    }

    @Test
    void recordsNumberOfModifiedViews() {
        migration.upgrade();

        final ArgumentCaptor<V20260908120000_StripReferencedSearchFilterAttributesInViews.MigrationCompleted> captor =
                ArgumentCaptor.forClass(V20260908120000_StripReferencedSearchFilterAttributesInViews.MigrationCompleted.class);
        verify(clusterConfigService).write(captor.capture());
        // views 1 and 3 have referenced filters with excess attributes; view 2 has none; view 4 is already clean
        assertThat(captor.getValue().modifiedViewCount()).isEqualTo(2);
    }

    private Document getFirstFilterOfFirstWidget(Document view) {
        final Document state = view.get("state", Document.class);
        final Document stateEntry = (Document) state.values().iterator().next();
        return stateEntry.getList("widgets", Document.class).get(0)
                .getList("filters", Document.class).get(0);
    }

    private List<Document> getWidgets(Document state, String stateKey) {
        return state.get(stateKey, Document.class).getList("widgets", Document.class);
    }
}
