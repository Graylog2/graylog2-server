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
package org.graylog.plugins.views;

import io.restassured.specification.RequestSpecification;
import org.graylog.storage.elasticsearch6.ElasticsearchInstanceES6Factory;
import org.graylog.testing.completebackend.ApiIntegrationTest;
import org.graylog.testing.completebackend.GraylogBackend;

import static org.graylog.testing.completebackend.Lifecycle.CLASS;

@ApiIntegrationTest(serverLifecycle = CLASS, elasticsearchFactory = ElasticsearchInstanceES6Factory.class)
class MessagesResourceES6IT extends MessagesResourceIT {
    public MessagesResourceES6IT(GraylogBackend sut, RequestSpecification requestSpec) {
        super(sut, requestSpec);
    }
}
