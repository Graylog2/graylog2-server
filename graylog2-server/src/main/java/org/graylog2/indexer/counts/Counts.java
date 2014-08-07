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
package org.graylog2.indexer.counts;

import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.elasticsearch.action.count.CountRequest;
import org.elasticsearch.client.Client;
import org.graylog2.indexer.Deflector;
import org.graylog2.indexer.Indexer;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class Counts {
    public interface Factory {
        Counts create(Client client);
    }

	private final Client c;
    private final Indexer indexer;
    private final Deflector deflector;

    @AssistedInject
    public Counts(@Assisted Client client, Deflector deflector, Indexer indexer) {
        this.deflector = deflector;
		this.c = client;
        this.indexer = indexer;
    }

    public long total() {
        return c.count(new CountRequest(deflector.getAllDeflectorIndexNames())).actionGet().getCount();
    }
	
}
