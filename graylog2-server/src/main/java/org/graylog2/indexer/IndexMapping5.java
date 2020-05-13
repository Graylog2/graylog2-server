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
package org.graylog2.indexer;

/**
 * Representing the message type mapping in Elasticsearch 5.x. This is giving ES more
 * information about what the fields look like and how it should analyze them.
 *
 * @see <a href="https://www.elastic.co/guide/en/elasticsearch/reference/5.4/mapping.html">Elasticsearch Reference / Mapping</a>
 */
class IndexMapping5 extends IndexMapping {}
