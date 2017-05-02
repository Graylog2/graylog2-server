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
package org.graylog2.rest;

import org.graylog2.indexer.ElasticsearchException;
import org.graylog2.rest.resources.search.responses.SearchError;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class ElasticsearchExceptionMapper implements ExceptionMapper<ElasticsearchException> {
    @Override
    public Response toResponse(ElasticsearchException exception) {
        final SearchError searchError = SearchError.create(exception.getMessage(), exception.getErrorDetails());

        return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(searchError).build();
    }
}
