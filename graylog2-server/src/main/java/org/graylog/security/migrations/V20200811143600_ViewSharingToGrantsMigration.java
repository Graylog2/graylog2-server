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
package org.graylog.security.migrations;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.graylog.plugins.views.search.views.ViewService;
import org.graylog.security.DBGrantService;
import org.graylog2.database.MongoConnection;
import org.graylog2.migrations.Migration;

import javax.inject.Inject;
import java.time.ZonedDateTime;
import java.util.function.Consumer;

public class V20200811143600_ViewSharingToGrantsMigration extends Migration {
    private final DBGrantService grantService;
    private final MongoCollection<Document> collection;
    private final ViewService viewService;

    @Inject
    public V20200811143600_ViewSharingToGrantsMigration(MongoConnection mongoConnection,
                                                        DBGrantService grantService,
                                                        ViewService viewService) {
        this.grantService = grantService;
        this.viewService = viewService;
        this.collection = mongoConnection.getMongoDatabase().getCollection("view_sharings", Document.class);
    }

    @Override
    public ZonedDateTime createdAt() {
        return ZonedDateTime.parse("2020-08-11T14:36:00Z");
    }

    @Override
    public void upgrade() {
        collection.find().forEach((Consumer<? super Document>) document -> {

        });
    }
}
