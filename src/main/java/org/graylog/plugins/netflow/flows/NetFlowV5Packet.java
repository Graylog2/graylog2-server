package org.graylog.plugins.netflow.flows;

import com.google.common.base.MoreObjects;
import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import org.graylog.plugins.netflow.utils.UUIDs;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import java.net.InetSocketAddress;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.graylog.plugins.netflow.utils.ByteBufUtils.getUnsignedInteger;

/**
 * NetFlow Version 5
 *
 * *-------*---------------*------------------------------------------------------*
 * | Bytes | Contents      | Description                                          |
 * *-------*---------------*------------------------------------------------------*
 * | 0-1   | version       | The version of NetFlow records exported 005          |
 * *-------*---------------*------------------------------------------------------*
 * | 2-3   | count         | Number of flows exported in this packet (1-30)       |
 * *-------*---------------*------------------------------------------------------*
 * | 4-7   | SysUptime     | Current time in milli since the export device booted |
 * *-------*---------------*------------------------------------------------------*
 * | 8-11  | unix_secs     | Current count of seconds since 0000 UTC 1970         |
 * *-------*---------------*------------------------------------------------------*
 * | 12-15 | unix_nsecs    | Residual nanoseconds since 0000 UTC 1970             |
 * *-------*---------------*------------------------------------------------------*
 * | 16-19 | flow_sequence | Sequence counter of total flows seen                 |
 * *-------*---------------*------------------------------------------------------*
 * | 20    | engine_type   | Type of flow-switching engine                        |
 * *-------*---------------*------------------------------------------------------*
 * | 21    | engine_id     | Slot number of the flow-switching engine             |
 * *-------*---------------*------------------------------------------------------*
 * | 22-23 | sampling_int  | First two bits hold the sampling mode                |
 * |       |               | remaining 14 bits hold value of sampling interval    |
 * *-------*---------------*------------------------------------------------------*
 */
public class NetFlowV5Packet implements NetFlowPacket {
    private static final int HEADER_SIZE = 24;
    private static final int FLOW_SIZE = 48;

    private static final String VERSION = "NetFlowV5 Packet";

    private final UUID id;
    private final InetSocketAddress sender;
    private final int length;
    private final long uptime;
    private final DateTime timestamp;
    private final List<NetFlow> flows;
    private final long flowSequence;
    private final int engineType;
    private final int engineId;
    private final int samplingInterval;
    private final int samplingMode;

    public NetFlowV5Packet(UUID id,
                           InetSocketAddress sender,
                           int length,
                           long uptime,
                           DateTime timestamp,
                           List<NetFlow> flows,
                           long flowSequence,
                           int engineType,
                           int engineId,
                           int samplingInterval,
                           int samplingMode) {

        this.id = id;
        this.sender = sender;
        this.length = length;
        this.uptime = uptime;
        this.timestamp = timestamp;
        this.flows = flows;
        this.flowSequence = flowSequence;
        this.engineType = engineType;
        this.engineId = engineId;
        this.samplingInterval = samplingInterval;
        this.samplingMode = samplingMode;
    }

    public Collection<NetFlow> getFlows() {
        return flows;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("id", id)
                .add("sender", sender)
                .add("length", length)
                .add("uptime", uptime)
                .add("timestamp", timestamp)
                .add("flows", flows)
                .add("flowSequence", flowSequence)
                .add("engineType", engineType)
                .add("engineId", engineId)
                .add("samplingInterval", samplingInterval)
                .add("samplingMode", samplingMode)
                .toString();
    }

    public static NetFlowV5Packet parse(InetSocketAddress sender, ByteBuf buf) throws FlowException {
        final int version = (int) getUnsignedInteger(buf, 0, 2);
        if (version != 5) {
            throw new InvalidFlowVersionException(version);
        }

        final int count = (int) getUnsignedInteger(buf, 2, 2);
        if (count <= 0 || buf.readableBytes() < HEADER_SIZE + count * FLOW_SIZE) {
            throw new CorruptFlowPacketException();
        }

        final long uptime = getUnsignedInteger(buf, 4, 4);
        final DateTime timestamp = new DateTime(getUnsignedInteger(buf, 8, 4) * 1000, DateTimeZone.UTC);
        final UUID id = UUIDs.startOf(timestamp.getMillis());
        final long flowSequence = getUnsignedInteger(buf, 16, 4);
        final int engineType = (int) getUnsignedInteger(buf, 20, 1);
        final int engineId = (int) getUnsignedInteger(buf, 21, 1);
        // the first 2 bits are the sampling mode, the remaining 14 the interval
        final int sampling = (int) getUnsignedInteger(buf, 22, 2);
        final int samplingInterval = sampling & 0x3FFF;
        final int samplingMode = sampling >> 14;

        final List<NetFlow> flows = Lists.newArrayListWithCapacity(count);
        for (int i = 0; i <= (count - 1); i++) {
            final NetFlow flowV5 = NetFlowV5.parse(sender,
                    buf.slice(HEADER_SIZE + (i * FLOW_SIZE), FLOW_SIZE),
                    id,
                    uptime,
                    timestamp,
                    samplingInterval,
                    samplingInterval > 0);
            flows.add(flowV5);
        }

        return new NetFlowV5Packet(id, sender, buf.readableBytes(), uptime, timestamp, flows, flowSequence, engineType, engineId, samplingInterval, samplingMode);
    }
}
