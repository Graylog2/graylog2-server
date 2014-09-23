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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.elasticsearch.action.count.CountRequest;
import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.graylog2.indexer.Deflector;

/**
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
@Singleton
public class Counts {
    private final Client c;
    private final Deflector deflector;

    @Inject
    public Counts(Node node, Deflector deflector) {
        this.c = node.client();
        this.deflector = deflector;
    }

    public long total() {
        return c.count(new CountRequest(deflector.getAllDeflectorIndexNames())).actionGet().getCount();
    }

}
