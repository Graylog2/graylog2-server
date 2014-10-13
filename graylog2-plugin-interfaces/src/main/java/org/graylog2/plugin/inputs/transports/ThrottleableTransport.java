/**
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
package org.graylog2.plugin.inputs.transports;

/**
 * Placeholder class for implementing logic to throttle certain transports which support backpressure.
 * The built in transports that support this by reading less are the Kafka and AMQP transports.
 * <br/>
 * Empty for now, since the process buffer provides natural throttling for now, but once that is async we need
 * to supply back pressure in some other way.
 */
public abstract class ThrottleableTransport implements Transport {

}
