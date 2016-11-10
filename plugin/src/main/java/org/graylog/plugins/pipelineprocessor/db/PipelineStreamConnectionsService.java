/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.db;

import org.graylog.plugins.pipelineprocessor.rest.PipelineConnections;
import org.graylog2.database.NotFoundException;

import java.util.Set;

public interface PipelineStreamConnectionsService {
    PipelineConnections save(PipelineConnections connections);

    PipelineConnections load(String streamId) throws NotFoundException;

    Set<PipelineConnections> loadAll();

    void delete(String streamId);
}
