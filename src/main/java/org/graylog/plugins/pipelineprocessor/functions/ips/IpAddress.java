package org.graylog.plugins.pipelineprocessor.functions.ips;

import java.net.InetAddress;

/**
 * Graylog's rule language wrapper for InetAddress.
 * <br/>
 * The purpose of this class is to guard against accidentally accessing properties which can trigger name resolutions
 * and to provide a known interface to deal with IP addresses.
 * <br/>
 * Almost all of the logic is in the actual InetAddress delegate object.
 */
public class IpAddress {

    private InetAddress address;

    public IpAddress(InetAddress address) {
        this.address = address;
    }


    public InetAddress inetAddress() {
        return address;
    }
}
