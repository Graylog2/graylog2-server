/*
 * Copyright 2013 TORCH UG
 *
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
package lib;

public class APIException extends Exception {

	private final int httpCode;
	private final String body;

	public APIException(int httpCode) {
        this(httpCode, null, (String)null);

    }

    public APIException(int httpCode, String msg) {
        this(httpCode, msg, (String) null);
    }

    public APIException(int httpCode, String msg, Throwable cause) {
        super(msg, cause);
        this.httpCode = httpCode;
        this.body = null;
    }


    public APIException(int httpCode, String msg, String body) {
		super(msg);
		this.httpCode = httpCode;
        this.body = body;
	}

    public String getBody() {
        return body;
    }

	public int getHttpCode() {
		return httpCode;
	}
	
}
