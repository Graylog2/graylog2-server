/**
 * This file is part of Graylog2.
 *
 * Graylog2 is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog2 is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog2.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog2.streams;

import com.google.inject.ImplementedBy;
import org.graylog2.database.NotFoundException;
import org.graylog2.database.PersistedService;
import org.graylog2.database.ValidationException;
import org.graylog2.plugin.streams.Output;
import org.graylog2.plugin.streams.Stream;
import org.graylog2.streams.outputs.CreateOutputRequest;

import java.util.Set;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
@ImplementedBy(OutputServiceImpl.class)
public interface OutputService extends PersistedService {
    Output load(String streamOutputId) throws NotFoundException;
    Set<Output> loadAll();
    Set<Output> loadForStream(Stream stream);
    Output create(Output request) throws ValidationException;
    Output create(CreateOutputRequest request, String userId) throws ValidationException;
    void destroy(Output model) throws NotFoundException;
}
