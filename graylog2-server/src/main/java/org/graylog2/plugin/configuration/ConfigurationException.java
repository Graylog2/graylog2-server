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
package org.graylog2.plugin.configuration;

/**
 *
 * @author Lennart Koopmann <lennart@socketfeed.com>
 */
public class ConfigurationException extends Exception {
    
    /**
	 * Re-generate if you modify the class structure.
	 */
	private static final long serialVersionUID = 8745066310811183675L;

	public ConfigurationException() {
        super();
    }
    
    public ConfigurationException(String msg) {
        super(msg);
    }

    public ConfigurationException(String msg, Throwable e) {
        super(msg, e);
    }
    
}