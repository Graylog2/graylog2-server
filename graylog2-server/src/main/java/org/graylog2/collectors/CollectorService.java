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
package org.graylog2.collectors;

import org.graylog2.rest.models.collector.requests.CollectorRegistrationRequest;

import java.util.List;
import java.util.concurrent.TimeUnit;

public interface CollectorService {
    long count();
    Collector save(Collector collector);
    List<Collector> all();
    Collector findById(String id);
    List<Collector> findByNodeId(String nodeId);
    int destroy(Collector collector);
    int destroyExpired(int time, TimeUnit unit);

    Collector fromRequest(String collectorId, CollectorRegistrationRequest request, String collectorVersion);
}
