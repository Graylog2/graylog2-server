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
package org.graylog2.shared.system.stats.network;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

@JsonAutoDetect
@AutoValue
public abstract class NetworkStats {
    @JsonProperty("primary_interface")
    @Nullable
    public abstract String primaryInterface();

    @JsonProperty("interfaces")
    public abstract Map<String, Interface> interfaces();

    @JsonProperty("tcp")
    @Nullable
    public abstract TcpStats tcp();

    public static NetworkStats create(String primaryInterface,
                                      Map<String, NetworkStats.Interface> interfaces,
                                      NetworkStats.TcpStats tcp) {
        return new AutoValue_NetworkStats(primaryInterface, interfaces, tcp);
    }

    @JsonAutoDetect
    @AutoValue
    public abstract static class Interface {
        @JsonProperty("name")
        public abstract String name();

        @JsonProperty("addresses")
        public abstract Set<String> addresses();

        @JsonProperty("mac_address")
        public abstract String macAddress();

        @JsonProperty("mtu")
        public abstract long mtu();

        @JsonProperty("interface_stats")
        @Nullable
        public abstract InterfaceStats interfaceStats();

        public static Interface create(String name,
                                       Set<String> addresses,
                                       String macAddress,
                                       long mtu,
                                       InterfaceStats interfaceStats) {
            return new AutoValue_NetworkStats_Interface(name, addresses, macAddress, mtu, interfaceStats);
        }
    }

    @JsonAutoDetect
    @AutoValue
    public abstract static class InterfaceStats {
        @JsonProperty("rx_packets")
        public abstract long rxPackets();

        @JsonProperty("rx_errors")
        public abstract long rxErrors();

        @JsonProperty("rx_dropped")
        public abstract long rxDropped();

        @JsonProperty("rx_overruns")
        public abstract long rxOverruns();

        @JsonProperty("rx_frame")
        public abstract long rxFrame();

        @JsonProperty("tx_packets")
        public abstract long txPackets();

        @JsonProperty("tx_errors")
        public abstract long txErrors();

        @JsonProperty("tx_dropped")
        public abstract long txDropped();

        @JsonProperty("tx_overruns")
        public abstract long txOverruns();

        @JsonProperty("tx_carrier")
        public abstract long txCarrier();

        @JsonProperty("tx_collisions")
        public abstract long txCollisions();

        @JsonProperty("rx_bytes")
        public abstract long rxBytes();

        @JsonProperty("tx_bytes")
        public abstract long txBytes();

        public static InterfaceStats create(long rxPackets,
                                            long rxErrors,
                                            long rxDropped,
                                            long rxOverruns,
                                            long rxFrame,
                                            long txPackets,
                                            long txErrors,
                                            long txDropped,
                                            long txOverruns,
                                            long txCarrier,
                                            long txCollisions,
                                            long rxBytes,
                                            long txBytes) {
            return new AutoValue_NetworkStats_InterfaceStats(
                    rxPackets, rxErrors, rxDropped, rxOverruns, rxFrame,
                    txPackets, txErrors, txDropped, txOverruns, txCarrier, txCollisions,
                    rxBytes, txBytes);
        }
    }

    @JsonAutoDetect
    @AutoValue
    public abstract static class TcpStats {
        @JsonProperty("active_opens")
        public abstract long activeOpens();

        @JsonProperty("passive_opens")
        public abstract long passiveOpens();

        @JsonProperty("attempt_fails")
        public abstract long attemptFails();

        @JsonProperty("estab_resets")
        public abstract long estabResets();

        @JsonProperty("curr_estab")
        public abstract long currEstab();

        @JsonProperty("in_segs")
        public abstract long inSegs();

        @JsonProperty("out_segs")
        public abstract long outSegs();

        @JsonProperty("retrans_segs")
        public abstract long retransSegs();

        @JsonProperty("in_errs")
        public abstract long inErrs();

        @JsonProperty("out_rsts")
        public abstract long outRsts();

        public static TcpStats create(long activeOpens,
                                      long passiveOpens,
                                      long attemptFails,
                                      long estabResets,
                                      long currEstab,
                                      long inSegs,
                                      long outSegs,
                                      long retransSegs,
                                      long inErrs,
                                      long outRsts) {
            return new AutoValue_NetworkStats_TcpStats(
                    activeOpens, passiveOpens, attemptFails, estabResets, currEstab,
                    inSegs, outSegs, retransSegs, inErrs, outRsts);
        }
    }
}
