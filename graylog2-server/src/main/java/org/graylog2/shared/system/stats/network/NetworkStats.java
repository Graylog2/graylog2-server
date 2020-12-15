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
import org.graylog.autovalue.WithBeanGetter;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Set;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class NetworkStats {
    @JsonProperty
    @Nullable
    public abstract String primaryInterface();

    @JsonProperty
    public abstract Map<String, Interface> interfaces();

    @JsonProperty
    @Nullable
    public abstract TcpStats tcp();

    public static NetworkStats create(String primaryInterface,
                                      Map<String, NetworkStats.Interface> interfaces,
                                      NetworkStats.TcpStats tcp) {
        return new AutoValue_NetworkStats(primaryInterface, interfaces, tcp);
    }

    @JsonAutoDetect
    @AutoValue
    @WithBeanGetter
    public abstract static class Interface {
        @JsonProperty
        public abstract String name();

        @JsonProperty
        public abstract Set<String> addresses();

        @JsonProperty
        public abstract String macAddress();

        @JsonProperty
        public abstract long mtu();

        @JsonProperty
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
    @WithBeanGetter
    public abstract static class InterfaceStats {
        @JsonProperty
        public abstract long rxPackets();

        @JsonProperty
        public abstract long rxErrors();

        @JsonProperty
        public abstract long rxDropped();

        @JsonProperty
        public abstract long rxOverruns();

        @JsonProperty
        public abstract long rxFrame();

        @JsonProperty
        public abstract long txPackets();

        @JsonProperty
        public abstract long txErrors();

        @JsonProperty
        public abstract long txDropped();

        @JsonProperty
        public abstract long txOverruns();

        @JsonProperty
        public abstract long txCarrier();

        @JsonProperty
        public abstract long txCollisions();

        @JsonProperty
        public abstract long rxBytes();

        @JsonProperty
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
    @WithBeanGetter
    public abstract static class TcpStats {
        @JsonProperty
        public abstract long activeOpens();

        @JsonProperty
        public abstract long passiveOpens();

        @JsonProperty
        public abstract long attemptFails();

        @JsonProperty
        public abstract long estabResets();

        @JsonProperty
        public abstract long currEstab();

        @JsonProperty
        public abstract long inSegs();

        @JsonProperty
        public abstract long outSegs();

        @JsonProperty
        public abstract long retransSegs();

        @JsonProperty
        public abstract long inErrs();

        @JsonProperty
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