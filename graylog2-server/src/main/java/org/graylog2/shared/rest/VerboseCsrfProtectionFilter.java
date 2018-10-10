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
package org.graylog2.shared.rest;

import org.glassfish.jersey.server.filter.CsrfProtectionFilter;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.container.ContainerRequestContext;
import java.io.IOException;

public class VerboseCsrfProtectionFilter extends CsrfProtectionFilter {
    @Override
    public void filter(ContainerRequestContext rc) throws IOException {
        try {
            super.filter(rc);
        } catch (BadRequestException badRequestException) {
            throw new BadRequestException(
                    "CSRF protection header is missing. Please add a \"" + HEADER_NAME + "\" header to your request.",
                    badRequestException
            );
        }
    }
}
