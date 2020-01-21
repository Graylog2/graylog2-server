/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.savedsearches;

import org.graylog.testing.mongodb.MongoDBFixtures;
import org.graylog.testing.mongodb.MongoDBInstance;
import org.graylog2.database.NotFoundException;
import org.graylog2.plugin.Tools;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class SavedSearchServiceImplTest {
    @Rule
    public final MongoDBInstance mongodb = MongoDBInstance.createForClass();

    private SavedSearchService savedSearchService;

    @Before
    public void setUpService() throws Exception {
        this.savedSearchService = new SavedSearchServiceImpl(mongodb.mongoConnection());
    }

    @Test
    public void testAllEmptyCollection() throws Exception {
        final List<SavedSearch> savedSearches = this.savedSearchService.all();

        assertNotNull("Returned list should not be null", savedSearches);
        assertEquals("Returned list should contain exactly one saved search", 0, savedSearches.size());
    }

    @Test
    @MongoDBFixtures("savedSearchSingleDocument.json")
    public void testAllSingleDocumentInCollection() throws Exception {
        final List<SavedSearch> savedSearches = this.savedSearchService.all();

        assertNotNull("Returned list should not be null", savedSearches);
        assertEquals("Returned list should contain exactly one saved search", 1, savedSearches.size());
    }

    @Test
    @MongoDBFixtures({"savedSearchSingleDocument.json", "savedSearchSingleDocument2.json"})
    public void testAllMultipleDocumentsInCollection() throws Exception {
        final List<SavedSearch> savedSearches = this.savedSearchService.all();

        assertNotNull("Returned list should not be null", savedSearches);
        assertEquals("Returned list should contain exactly one saved search", 2, savedSearches.size());
    }

    @Test
    @MongoDBFixtures({"savedSearchSingleDocument.json", "savedSearchSingleDocument2.json"})
    public void testLoad() throws Exception {
        final String id1 = "5400deadbeefdeadbeefaffe";
        final SavedSearch savedSearch = savedSearchService.load(id1);

        assertNotNull("Should return a saved search", savedSearch);
        assertEquals("Should return the saved search with the correct id", id1, savedSearch.getId());

        final String id2 = "54e3deadbeefdeadbeefaffe";
        final SavedSearch savedSearch2 = savedSearchService.load(id2);

        assertNotNull("Should return a saved search", savedSearch2);
        assertEquals("Should return the saved search with the correct id", id2, savedSearch2.getId());
    }

    @Test(expected = NotFoundException.class)
    public void testLoadNonexisting() throws Exception {
        savedSearchService.load("54e3deadbeefdeadbeefaffe");
    }

    @Test(expected = IllegalArgumentException.class)
    public void testLoadInvalidId() throws Exception {
        savedSearchService.load("foobar");
    }

    @Test
    public void testCreate() throws Exception {
        final String title = "Example Title";
        final Map<String, Object> query =Collections.emptyMap();
        final String creatorUserId = "someuser";
        final DateTime createdAt = Tools.nowUTC();

        final SavedSearch savedSearch = savedSearchService.create(title, query, creatorUserId, createdAt);

        assertNotNull("Should have returned a SavedSearch", savedSearch);
        assertEquals("Collection should still be empty", 0, savedSearchService.all().size());
    }
}
