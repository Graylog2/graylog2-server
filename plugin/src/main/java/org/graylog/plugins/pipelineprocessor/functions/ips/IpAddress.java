/**
 * This file is part of Graylog Pipeline Processor.
 *
 * Graylog Pipeline Processor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog Pipeline Processor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog Pipeline Processor.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.graylog.plugins.pipelineprocessor.functions.ips;

import com.google.common.net.InetAddresses;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

/**
 * Graylog's rule language wrapper for {@link InetAddress}.
 * <br>
 * The purpose of this class is to guard against accidentally accessing properties which can trigger name resolutions
 * and to provide a known interface to deal with IP addresses.
 * <br>
 * Almost all of the logic is in the actual {@link InetAddress} delegate object.
 */
public class IpAddress {

    private InetAddress address;

    public IpAddress(InetAddress address) {
        this.address = address;
    }

    public InetAddress inetAddress() {
        return address;
    }

    @Override
    public String toString() {
        return InetAddresses.toAddrString(address);
    }

    @SuppressWarnings("unused")
    public IpAddress getAnonymized() {
        final byte[] address = this.address.getAddress();
        address[address.length-1] = 0x00;
        try {
            return new IpAddress(InetAddress.getByAddress(address));
        } catch (UnknownHostException e) {
            // cannot happen, it's created from a valid InetAddress to begin with
            throw new IllegalStateException(e);
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof IpAddress)) return false;
        IpAddress ipAddress = (IpAddress) o;
        return Objects.equals(address, ipAddress.address);
    }

    @Override
    public int hashCode() {
        return Objects.hash(address);
    }
}
