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
package org.graylog2.indexer.results;

public class IndexQueryResult {
    private final String originalQuery;
    private final long tookMs;
    private final String builtQuery;

    public IndexQueryResult(String originalQuery, String builtQuery, long tookMs) {
        this.originalQuery = originalQuery;
        this.tookMs = tookMs;
        this.builtQuery = builtQuery;
    }

    public String getOriginalQuery() {
        return originalQuery;
    }

    public String getBuiltQuery() {
        return builtQuery;
    }

    public long tookMs() {
        return tookMs;
    }
}
