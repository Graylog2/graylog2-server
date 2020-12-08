/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
/*
* The MIT License
*
* Copyright (c) 2013 Edin Dazdarevic (edin.dazdarevic@gmail.com)

* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:

* The above copyright notice and this permission notice shall be included in
* all copies or substantial portions of the Software.

* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
* THE SOFTWARE.
*
* */
package org.graylog2.utilities;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;
import java.util.StringJoiner;

/**
 * A class that enables to get an IP range from CIDR specification. It supports
 * both IPv4 and IPv6.
 */
public class IpSubnet {
    private static final byte[] MASK_IPV4 = {-1, -1, -1, -1};
    private static final byte[] MASK_IPV6 = {-1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1};

    private final InetAddress inetAddress;
    private InetAddress startAddress;
    private InetAddress endAddress;
    private final int prefixLength;


    public IpSubnet(String cidr) throws UnknownHostException {
        Objects.requireNonNull(cidr, "CIDR must not be null");

        /* split CIDR to address and prefix part */
        if (cidr.contains("/")) {
            int index = cidr.indexOf("/");
            String addressPart = cidr.substring(0, index);
            String networkPart = cidr.substring(index + 1);

            inetAddress = InetAddress.getByName(addressPart);
            prefixLength = Integer.parseInt(networkPart);

            calculate();
        } else {
            throw new UnknownHostException("Invalid subnet: " + cidr);
        }
    }

    private void calculate() throws UnknownHostException {
        final int targetSize;
        final BigInteger mask;
        if (inetAddress.getAddress().length == 4) {
            targetSize = 4;
            mask = (new BigInteger(1, MASK_IPV4)).not().shiftRight(prefixLength);
        } else {
            targetSize = 16;
            mask = (new BigInteger(1, MASK_IPV6)).not().shiftRight(prefixLength);
        }

        final BigInteger ipVal = new BigInteger(1, inetAddress.getAddress());

        final BigInteger startIp = ipVal.and(mask);
        final BigInteger endIp = startIp.add(mask.not());

        final byte[] startIpArr = toBytes(startIp.toByteArray(), targetSize);
        final byte[] endIpArr = toBytes(endIp.toByteArray(), targetSize);

        this.startAddress = InetAddress.getByAddress(startIpArr);
        this.endAddress = InetAddress.getByAddress(endIpArr);
    }

    private byte[] toBytes(byte[] array, int targetSize) {
        final byte[] b = new byte[targetSize];
        final int length = (targetSize > array.length) ? array.length : targetSize;
        final int srcPos = (array.length > targetSize) ? array.length - targetSize : 0;
        final int destPos = (targetSize > array.length) ? targetSize - array.length : 0;

        System.arraycopy(array, srcPos, b, destPos, length);

        return b;
    }

    public String getNetworkAddress() {
        return this.startAddress.getHostAddress();
    }

    public String getBroadcastAddress() {
        return this.endAddress.getHostAddress();
    }

    public boolean contains(String ipAddress) throws UnknownHostException {
        return contains(InetAddress.getByName(ipAddress));
    }

    public boolean contains(InetAddress address) {
        final BigInteger start = new BigInteger(1, this.startAddress.getAddress());
        final BigInteger end = new BigInteger(1, this.endAddress.getAddress());
        final BigInteger target = new BigInteger(1, address.getAddress());

        final int st = start.compareTo(target);
        final int te = target.compareTo(end);

        return (st == -1 || st == 0) && (te == -1 || te == 0);
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        IpSubnet that = (IpSubnet) o;

        return Objects.equals(this.startAddress, that.startAddress) &&
                Objects.equals(this.endAddress, that.endAddress) &&
                Objects.equals(this.prefixLength, that.prefixLength);
    }

    @Override
    public int hashCode() {
        return Objects.hash(startAddress, endAddress, prefixLength);
    }

    @Override
    public String toString() {
        return inetAddress.getHostAddress() + "/" + prefixLength;
    }
}