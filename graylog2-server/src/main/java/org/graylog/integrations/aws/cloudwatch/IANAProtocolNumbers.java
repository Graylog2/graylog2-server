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
package org.graylog.integrations.aws.cloudwatch;

import com.google.common.collect.ImmutableMap;

/**
 * Resolves IANA protocol numbers to their names.
 */
public class IANAProtocolNumbers {

    private final ImmutableMap<Integer, String> table;

    public IANAProtocolNumbers() {
        this.table = ImmutableMap.<Integer, String>builder()
                .put(0, "IP")
                .put(1, "ICMP")
                .put(2, "IGMP")
                .put(3, "GGP")
                .put(4, "IP-ENCAP")
                .put(5, "ST2")
                .put(6, "TCP")
                .put(7, "CBT")
                .put(8, "EGP")
                .put(9, "IGP")
                .put(10, "BBN-RCC-MON")
                .put(11, "NVP-II")
                .put(12, "PUP")
                .put(13, "ARGUS")
                .put(14, "EMCON")
                .put(15, "XNET")
                .put(16, "CHAOS")
                .put(17, "UDP")
                .put(18, "MUX")
                .put(19, "DCN-MEAS")
                .put(20, "HMP")
                .put(21, "PRM")
                .put(22, "XNS-IDP")
                .put(23, "TRUNK-1")
                .put(24, "TRUNK-2")
                .put(25, "LEAF-1")
                .put(26, "LEAF-2")
                .put(27, "RDP")
                .put(28, "IRTP")
                .put(29, "ISO-TP4")
                .put(30, "NETBLT")
                .put(31, "MFE-NSP")
                .put(32, "MERIT-INP")
                .put(33, "SEP")
                .put(34, "3PC")
                .put(35, "IDPR")
                .put(36, "XTP")
                .put(37, "DDP")
                .put(38, "IDPR-CMTP")
                .put(39, "TP++")
                .put(40, "IL")
                .put(41, "IPV6")
                .put(42, "SDRP")
                .put(43, "IPV6-ROUTE")
                .put(44, "IPV6-FRAG")
                .put(45, "IDRP")
                .put(46, "RSVP")
                .put(47, "GRE")
                .put(48, "MHRP")
                .put(49, "BNA")
                .put(50, "ESP")
                .put(51, "AH")
                .put(52, "I-NLSP")
                .put(53, "SWIPE")
                .put(54, "NARP")
                .put(55, "MOBILE")
                .put(56, "TLSP")
                .put(57, "SKIP")
                .put(58, "IPV6-ICMP")
                .put(59, "IPV6-NONXT")
                .put(60, "IPV6-OPTS")
                .put(62, "CFTP")
                .put(64, "SAT-EXPAK")
                .put(65, "KRYPTOLAN")
                .put(66, "RVD")
                .put(67, "IPPC")
                .put(69, "SAT-MON")
                .put(70, "VISA")
                .put(71, "IPCV")
                .put(72, "CPNX")
                .put(73, "CPHB")
                .put(74, "WSN")
                .put(75, "PVP")
                .put(76, "BR-SAT-MON")
                .put(77, "SUN-ND")
                .put(78, "WB-MON")
                .put(79, "WB-EXPAK")
                .put(80, "ISO-IP")
                .put(81, "VMTP")
                .put(82, "SECURE-VMTP")
                .put(83, "VINES")
                .put(84, "TTP")
                .put(85, "NSFNET-IGP")
                .put(86, "DGP")
                .put(87, "TCF")
                .put(88, "EIGRP")
                .put(89, "OSPFIGP")
                .put(90, "Sprite-RPC")
                .put(91, "LARP")
                .put(92, "MTP")
                .put(93, "AX.25")
                .put(94, "IPIP")
                .put(95, "MICP")
                .put(96, "SCC-SP")
                .put(97, "ETHERIP")
                .put(98, "ENCAP")
                .put(100, "GMTP")
                .put(101, "IFMP")
                .put(102, "PNNI")
                .put(103, "PIM")
                .put(104, "ARIS")
                .put(105, "SCPS")
                .put(106, "QNX")
                .put(107, "A/N")
                .put(108, "IPComp")
                .put(109, "SNP")
                .put(110, "Compaq-Peer")
                .put(111, "IPX-in-IP")
                .put(112, "VRRP")
                .put(113, "PGM")
                .put(115, "2TP")
                .put(116, "DDX")
                .put(117, "IATP")
                .put(118, "ST")
                .put(119, "SRP")
                .put(120, "UTI")
                .put(121, "SMP")
                .put(122, "SM")
                .put(123, "PTP")
                .put(124, "ISIS")
                .put(125, "FIRE")
                .put(126, "CRTP")
                .put(127, "CRUDP")
                .put(128, "SSCOPMCE")
                .put(129, "IPLT")
                .put(130, "SPS")
                .put(131, "PIPE")
                .put(132, "SCTP")
                .put(133, "FC")
                .put(136, "UDPLite")
                .put(137, "MPLS-in-IP")
                .put(138, "manet")
                .put(139, "HIP")
                .put(140, "Shim6")
                .put(141, "WESP")
                .put(142, "ROHC")
                .put(254, "DIVERT")
                .build();
    }

    public String lookup(int number) {
        return table.getOrDefault(number, "UNKNOWN");
    }
}
