/**
 * Copyright (C) 2012, 2013, 2014 wasted.io Ltd <really@wasted.io>
 * Copyright (C) 2015 Graylog, Inc. (hello@graylog.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.graylog.plugins.netflow.flows;

import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import io.netty.buffer.ByteBuf;
import org.graylog.plugins.netflow.utils.ByteBufUtils;
import org.graylog.plugins.netflow.utils.UUIDs;
import org.graylog2.plugin.Message;
import org.joda.time.DateTime;

import javax.annotation.Nullable;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.UUID;

import static org.graylog.plugins.netflow.utils.ByteBufUtils.getInetAddress;
import static org.graylog.plugins.netflow.utils.ByteBufUtils.getUnsignedInteger;

public class NetFlowV5 implements NetFlow {
    private static final int NF_VERSION = 5;
    private static final String VERSION = "NetFlowV5";

    private static final String MF_VERSION = "nf_version";
    private static final String MF_ID = "nf_id";
    private static final String MF_FLOW_PACKET_ID = "nf_flow_packet_id";
    private static final String MF_TOS = "nf_tos";
    private static final String MF_SRC_ADDRESS = "nf_src_address";
    private static final String MF_DST_ADDRESS = "nf_dst_address";
    private static final String MF_NEXT_HOP = "nf_next_hop";
    private static final String MF_SRC_PORT = "nf_src_port";
    private static final String MF_DST_PORT = "nf_dst_port";
    private static final String MF_SRC_MASK = "nf_src_mask";
    private static final String MF_DST_MASK = "nf_dst_mask";
    private static final String MF_PROTO = "nf_proto";
    private static final String MF_TCP_FLAGS = "nf_tcp_flags";
    private static final String MF_START = "nf_start";
    private static final String MF_STOP = "nf_stop";
    private static final String MF_BYTES = "nf_bytes";
    private static final String MF_PKTS = "nf_pkts";

    public final UUID uuid;
    public final InetSocketAddress sender;
    public final int length;
    public final long uptime;
    public final DateTime timestamp;
    public final int srcPort;
    public final int dstPort;
    public final Optional<Integer> srcAS;
    public final Optional<Integer> dstAS;
    public final long pkts;
    public final long bytes;
    public final int proto;
    public final int tos;
    public final int tcpflags;
    public final Optional<DateTime> start;
    public final Optional<DateTime> stop;
    public final InetAddress srcAddress;
    public final InetAddress dstAddress;
    public final Optional<InetAddress> nextHop;
    public final int snmpInput;
    public final int snmpOutput;
    public final int srcMask;
    public final int dstMask;
    public final UUID fpId;

    public NetFlowV5(UUID uuid,
                     InetSocketAddress sender,
                     int length,
                     long uptime,
                     DateTime timestamp,
                     int srcPort,
                     int dstPort,
                     Optional<Integer> srcAS,
                     Optional<Integer> dstAS,
                     long pkts,
                     long bytes,
                     int proto,
                     int tos,
                     int tcpflags,
                     Optional<DateTime> start,
                     Optional<DateTime> stop,
                     InetAddress srcAddress,
                     InetAddress dstAddress,
                     Optional<InetAddress> nextHop,
                     int snmpInput,
                     int snmpOutput,
                     int srcMask,
                     int dstMask,
                     UUID fpId) {

        this.uuid = uuid;
        this.sender = sender;
        this.length = length;
        this.uptime = uptime;
        this.timestamp = timestamp;
        this.srcPort = srcPort;
        this.dstPort = dstPort;
        this.srcAS = srcAS;
        this.dstAS = dstAS;
        this.pkts = pkts;
        this.bytes = bytes;
        this.proto = proto;
        this.tos = tos;
        this.tcpflags = tcpflags;
        this.start = start;
        this.stop = stop;
        this.srcAddress = srcAddress;
        this.dstAddress = dstAddress;
        this.nextHop = nextHop;
        this.snmpInput = snmpInput;
        this.snmpOutput = snmpOutput;
        this.srcMask = srcMask;
        this.dstMask = dstMask;
        this.fpId = fpId;
    }

    /**
     * Parse a Version 5 Flow
     *
     * @param sender           The sender's InetSocketAddress
     * @param buf              Netty ByteBuf Slice containing the UDP Packet
     * @param fpId             FlowPacket-UUID this Flow arrived on
     * @param uptime           Millis since UNIX Epoch when the exporting device/sender booted
     * @param timestamp        DateTime when this flow was exported
     * @param samplingInterval Interval samples are sent
     * @param calculateSamples Switch to turn on/off samples calculation
     */
    public static NetFlow parse(final InetSocketAddress sender,
                                  final ByteBuf buf,
                                  final UUID fpId,
                                  final long uptime,
                                  final DateTime timestamp,
                                  final int samplingInterval,
                                  final boolean calculateSamples) {

        final boolean sampling = calculateSamples;
        final long pkts = getUnsignedInteger(buf, 16, 4);
        final long bytes = getUnsignedInteger(buf, 20, 4);

        final int srcPort = (int) getUnsignedInteger(buf, 32, 2);
        final int dstPort = (int) getUnsignedInteger(buf, 34, 2);
        final int srcAS = (int) getUnsignedInteger(buf, 40, 2);
        final int dstAS = (int) getUnsignedInteger(buf, 42, 2);
        final int proto = buf.getUnsignedByte(38);
        final int tos = buf.getUnsignedByte(39);
        final int tcpflags = buf.getUnsignedByte(37);
        final long start = getUnsignedInteger(buf, 24, 4);
        final long stop = getUnsignedInteger(buf, 28, 4);
        final InetAddress srcAddress = getInetAddress(buf, 0, 4);
        final InetAddress dstAddress = getInetAddress(buf, 4, 4);
        final InetAddress nextHop = getInetAddress(buf, 8, 4);
        final int snmpInput = (int) getUnsignedInteger(buf, 12, 2);
        final int snmpOutput = (int) getUnsignedInteger(buf, 14, 2);
        final int srcMask = buf.getUnsignedByte(44);
        final int dstMask = buf.getUnsignedByte(45);


        return new NetFlowV5(UUIDs.timeBased(),
                sender,
                buf.readableBytes(),
                uptime,
                timestamp,
                srcPort,
                dstPort,
                srcAS != -1 ? Optional.of(srcAS) : Optional.<Integer>absent(),
                dstAS != -1 ? Optional.of(dstAS) : Optional.<Integer>absent(),
                sampling ? pkts * samplingInterval : pkts, // pkts
                sampling ? bytes * samplingInterval : bytes, // bytes
                proto,
                tos,
                tcpflags,
                start != 0 ? Optional.of(timestamp.minus(uptime - start)) : Optional.<DateTime>absent(), // start
                stop != 0 ? Optional.of(timestamp.minus(uptime - stop)) : Optional.<DateTime>absent(), // stop
                srcAddress,
                dstAddress,
                ByteBufUtils.DEFAULT_INET_ADDRESS.equals(nextHop.getHostAddress()) ? Optional.<InetAddress>absent() : Optional.of(nextHop), // nextHop
                snmpInput,
                snmpOutput,
                srcMask,
                dstMask,
                fpId);
    }

    @Override
    @Nullable
    public Message toMessage() {
        final String source = sender.getAddress().getHostAddress();
        final Message message = new Message(toMessageString(), source, timestamp);

        message.addField(MF_VERSION, NF_VERSION);
        message.addField(MF_ID, uuid.toString());
        message.addField(MF_FLOW_PACKET_ID, fpId.toString());
        message.addField(MF_TOS, tos);
        message.addField(MF_SRC_ADDRESS, srcAddress.getHostAddress()); // TODO Check if this does a DNS lookup!
        message.addField(MF_DST_ADDRESS, dstAddress.getHostAddress()); // TODO Check if this does a DNS lookup!
        if (nextHop.isPresent()) {
            message.addField(MF_NEXT_HOP, nextHop.get().getHostAddress()); // TODO Check if this does a DNS lookup!
        }
        message.addField(MF_SRC_PORT, srcPort);
        message.addField(MF_DST_PORT, dstPort);
        message.addField(MF_SRC_MASK, srcMask);
        message.addField(MF_DST_MASK, dstMask);
        message.addField(MF_PROTO, proto);
        message.addField(MF_TCP_FLAGS, tcpflags);
        if (start.isPresent()) {
            message.addField(MF_START, start.get());
        }
        if (stop.isPresent()) {
            message.addField(MF_STOP, stop.get());
        }
        message.addField(MF_BYTES, bytes);
        message.addField(MF_PKTS, pkts);

        return message;
    }

    @Override
    public String toMessageString() {
        return VERSION +" [" + srcAddress.getHostAddress() + "]:" + srcPort +
                " <> [" + dstAddress.getHostAddress() + "]:" + dstPort +
                " proto:" + proto + " pkts:" + pkts + " bytes:" + bytes;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("uuid", uuid)
                .add("sender", sender)
                .add("length", length)
                .add("uptime", uptime)
                .add("timestamp", timestamp)
                .add("srcPort", srcPort)
                .add("dstPort", dstPort)
                .add("srcAS", srcAS)
                .add("dstAS", dstAS)
                .add("pkts", pkts)
                .add("bytes", bytes)
                .add("proto", proto)
                .add("tos", tos)
                .add("tcpflags", tcpflags)
                .add("start", start)
                .add("stop", stop)
                .add("srcAddress", srcAddress)
                .add("dstAddress", dstAddress)
                .add("nextHop", nextHop)
                .add("snmpInput", snmpInput)
                .add("snmpOutput", snmpOutput)
                .add("srcMask", srcMask)
                .add("dstMask", dstMask)
                .add("fpId", fpId)
                .toString();
    }
}
