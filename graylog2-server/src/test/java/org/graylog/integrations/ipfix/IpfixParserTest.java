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
package org.graylog.integrations.ipfix;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import com.google.common.primitives.Bytes;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import static com.google.common.collect.Maps.immutableEntry;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

public class IpfixParserTest {

    private InformationElementDefinitions definitions = new InformationElementDefinitions(
            Resources.getResource("ipfix-iana-elements.json")
    );

    @Rule
    public final TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test(timeout = 5000)
    public void parseMessage_undefinedInformationElement_doesNotLoop() {
        final byte[] packetBytes = hexToBytes("000a00240000000000000001000000000002000c0100000103e800040100000841414141");
        final ByteBuf packet = Unpooled.wrappedBuffer(packetBytes);
        final IpfixMessage message = new IpfixParser(definitions).parseMessage(packet);
        assertThat(message).isNotNull();
        assertThat(message.flows()).hasSize(1);
        assertThat(message.flows().getFirst().fields()).isEmpty();
    }

    @Test(timeout = 5000)
    public void parseMessage_undefinedInformationElement_fixedLength_doesNotCorruptSubsequentField() {
        // Template: IE 1000 (len=4, undefined) + IE 8 (sourceIPv4Address, len=4, defined)
        // Data record: 4 junk bytes for IE 1000, then 192.168.1.1 for IE 8
        final byte[] packetBytes = hexToBytes("000a002c000000000000000100000000000200100100000203e8000400080004" +
                "0100000c41414141c0a80101");
        final ByteBuf packet = Unpooled.wrappedBuffer(packetBytes);
        final IpfixMessage message = new IpfixParser(definitions).parseMessage(packet);
        assertThat(message).isNotNull();
        assertThat(message.flows()).hasSize(1);
        assertThat(message.flows().getFirst().fields()).containsEntry("sourceIPv4Address", "192.168.1.1");
    }

    @Test(timeout = 5000)
    public void parseMessage_undefinedInformationElement_variableLength_doesNotCorruptSubsequentField() {
        // Template: IE 1000 (len=65535 var-length, undefined) + IE 8 (sourceIPv4Address, len=4, defined)
        // Data record: 0x04 (var-length prefix=4) + 4 junk bytes, then 192.168.1.1 for IE 8
        final byte[] packetBytes = hexToBytes("000a002d000000000000000100000000000200100100000203e8ffff00080004" +
                "0100000d0441414141c0a80101");
        final ByteBuf packet = Unpooled.wrappedBuffer(packetBytes);
        final IpfixMessage message = new IpfixParser(definitions).parseMessage(packet);
        assertThat(message).isNotNull();
        assertThat(message.flows()).hasSize(1);
        assertThat(message.flows().getFirst().fields()).containsEntry("sourceIPv4Address", "192.168.1.1");
    }

    @Test(timeout = 5000)
    public void parseMessage_basicListZeroLengthElement_doesNotLoop() {
        final byte[] packetBytes = hexToBytes("000a00270000000000000001000000000002000c01000001012300080100000b06000001000041");
        final ByteBuf packet = Unpooled.wrappedBuffer(packetBytes);
        final IpfixMessage message = new IpfixParser(definitions).parseMessage(packet);
        assertThat(message).isNotNull();
        assertThat(message.flows()).hasSize(1);
        assertThat(message.flows().getFirst().fields()).isEmpty();
    }

    @Test(timeout = 5000)
    public void parseMessage_deeplyNestedSubTemplateList_doesNotOverflowStack() {
        // A subTemplateList template that references itself recurses once per nesting level. Without a depth bound a
        // crafted packet exhausts the stack (StackOverflowError); the parser must bail cleanly at MAX_SUBTEMPLATE_DEPTH.
        final ByteBuf packet = Unpooled.wrappedBuffer(nestedSubTemplateListPacket(5000));
        final IpfixMessage message = new IpfixParser(definitions).parseMessage(packet);
        assertThat(message).isNotNull();
    }

    private static byte[] hexToBytes(final String hex) {
        final int len = hex.length();
        final byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    private static byte[] u16(int v) {
        return new byte[]{(byte) (v >> 8), (byte) v};
    }

    private static byte[] varLen(int n) {
        return n < 255 ? new byte[]{(byte) n} : Bytes.concat(new byte[]{(byte) 0xff}, u16(n));
    }

    // An IPFIX message whose template 256 is a single subTemplateList (IE 292) referencing template 256, nested `depth`
    // levels deep — the self-recursive parse path guarded by MAX_SUBTEMPLATE_DEPTH.
    private static byte[] nestedSubTemplateListPacket(int depth) {
        byte[] level = new byte[0];
        for (int i = 0; i < depth; i++) {
            final byte[] content = Bytes.concat(new byte[]{0x00}, u16(256), level); // semantic + templateId + nested list
            level = Bytes.concat(varLen(content.length), content);
        }
        final byte[] dataSet = Bytes.concat(u16(256), u16(4 + level.length), level);
        final byte[] template = Bytes.concat(u16(256), u16(1), u16(292), u16(0xffff));
        final byte[] templateSet = Bytes.concat(u16(2), u16(4 + template.length), template);
        final byte[] body = Bytes.concat(templateSet, dataSet);
        final byte[] header = Bytes.concat(u16(10), u16(16 + body.length),
                new byte[]{0, 0, 0, 1}, new byte[]{0, 0, 0, 1}, new byte[]{0, 0, 0, 1});
        return Bytes.concat(header, body);
    }

    @Test
    public void shallowParsePacket() throws IOException {
        final ByteBuf packet = Utils.readPacket("templates-data.ipfix");

        final IpfixParser.MessageDescription description = new IpfixParser(definitions).shallowParseMessage(packet);

        assertThat(description).isNotNull();
        assertThat(description.referencedTemplateIds()).contains(256);
        assertThat(description.declaredTemplateIds()).contains(256, 257);
        assertThat(description.declaredOptionsTemplateIds()).contains(258);
    }

    @Test
    public void shallowParsePacketMultilist() throws IOException {
        ByteBuf packet = Utils.readPacket("ixia-multilist.ipfix");
        InformationElementDefinitions definitions = new InformationElementDefinitions(
                Resources.getResource("ipfix-iana-elements.json"),
                Resources.getResource("ixia-ied.json")
        );
        final IpfixParser.MessageDescription description = new IpfixParser(definitions).shallowParseMessage(packet);

        assertThat(description).isNotNull();
        // this also refers to template id 257, but because we don't have the data-template set for 256 we don't know that
        // there is a multilist element which then refers to 257 (we'd have to parse the data set first to know that)
        assertThat(description.referencedTemplateIds()).contains(256);
    }

    @Test
    public void notIpfixMessage() throws IOException {
        final ByteBuf packet = Utils.readPacket("netflow-v9.dat");
        try {
            new IpfixParser(definitions).shallowParseMessage(packet);
            fail("Wrong netflow version");
        } catch (InvalidMessageVersion e) {
            assertThat(e).hasMessageContaining("9");
        } catch (Exception e) {
            fail("Unexpected exception thrown", e);
        }
    }

    @Test
    public void onlyDataSets() throws IOException {
        final ByteBuf packet = Utils.readPacket("dataset-only.ipfix");

        final IpfixParser.MessageDescription description = new IpfixParser(definitions).shallowParseMessage(packet);

        assertThat(description.templateRecords()).isEmpty();
        assertThat(description.optionsTemplateRecords()).isEmpty();
        assertThat(description.dataSets())
                .hasSize(1)
                .extracting(ShallowDataSet::templateId).containsExactly(256);
    }

    //@Ignore("Missing ied for private enterprise number 3054 failure.")
    @Test
    public void parseDataSet() throws IOException {

        final ByteBuf packet = Utils.readPacket("templates-data.ipfix");
        InformationElementDefinitions infoElementDefs = new InformationElementDefinitions(Resources.getResource("ipfix-iana-elements.json"),
                                                                                          Resources.getResource("ixia-ied.json"));

        final IpfixMessage ipfixMessage = new IpfixParser(infoElementDefs).parseMessage(packet);
        assertThat(ipfixMessage).isNotNull();

        final ImmutableList<TemplateRecord> templateRecords = ipfixMessage.templateRecords();
        assertThat(templateRecords).hasSize(2);

        final TemplateRecord firstTemplate = templateRecords.get(0);
        assertThat(firstTemplate.templateId()).isEqualTo(256);
        assertThat(firstTemplate.informationElements()).hasSize(51)
                                                       .containsExactly(
                                                               InformationElement.create(1, 8, 0),
                                                               InformationElement.create(2, 8, 0),
                                                               InformationElement.create(4, 1, 0),
                                                               InformationElement.create(6, 1, 0),
                                                               InformationElement.create(7, 2, 0),
                                                               InformationElement.create(8, 4, 0),
                                                               InformationElement.create(10, 4, 0),
                                                               InformationElement.create(11, 2, 0),
                                                               InformationElement.create(12, 4, 0),
                                                               InformationElement.create(14, 4, 0),
                                                               InformationElement.create(16, 4, 0),
                                                               InformationElement.create(17, 4, 0),
                                                               InformationElement.create(32, 2, 0),
                                                               InformationElement.create(136, 1, 0),
                                                               InformationElement.create(152, 8, 0),
                                                               InformationElement.create(153, 8, 0),
                                                               InformationElement.create(110, 4, 3054),
                                                               InformationElement.create(111, 65535, 3054),
                                                               InformationElement.create(120, 4, 3054),
                                                               InformationElement.create(121, 65535, 3054),
                                                               InformationElement.create(122, 4, 3054),
                                                               InformationElement.create(123, 65535, 3054),
                                                               InformationElement.create(125, 65535, 3054),
                                                               InformationElement.create(126, 4, 3054),
                                                               InformationElement.create(127, 4, 3054),
                                                               InformationElement.create(140, 4, 3054),
                                                               InformationElement.create(141, 65535, 3054),
                                                               InformationElement.create(142, 4, 3054),
                                                               InformationElement.create(143, 65535, 3054),
                                                               InformationElement.create(145, 65535, 3054),
                                                               InformationElement.create(146, 4, 3054),
                                                               InformationElement.create(147, 4, 3054),
                                                               InformationElement.create(160, 1, 3054),
                                                               InformationElement.create(161, 65535, 3054),
                                                               InformationElement.create(162, 1, 3054),
                                                               InformationElement.create(163, 65535, 3054),
                                                               InformationElement.create(178, 65535, 3054),
                                                               InformationElement.create(179, 65535, 3054),
                                                               InformationElement.create(180, 2, 3054),
                                                               InformationElement.create(182, 65535, 3054),
                                                               InformationElement.create(183, 65535, 3054),
                                                               InformationElement.create(184, 65535, 3054),
                                                               InformationElement.create(185, 65535, 3054),
                                                               InformationElement.create(186, 65535, 3054),
                                                               InformationElement.create(187, 65535, 3054),
                                                               InformationElement.create(188, 4, 3054),
                                                               InformationElement.create(189, 65535, 3054),
                                                               InformationElement.create(190, 65535, 3054),
                                                               InformationElement.create(191, 65535, 3054),
                                                               InformationElement.create(192, 65535, 3054),
                                                               InformationElement.create(193, 4, 3054)
                                                       );

        final TemplateRecord secondTemplate = templateRecords.get(1);
        assertThat(secondTemplate.templateId()).isEqualTo(257);
        assertThat(secondTemplate.informationElements())
                .hasSize(50)
                .containsOnly(
                        InformationElement.create(1, 8, 0),
                        InformationElement.create(2, 8, 0),
                        InformationElement.create(4, 1, 0),
                        InformationElement.create(6, 1, 0),
                        InformationElement.create(7, 2, 0),
                        InformationElement.create(10, 4, 0),
                        InformationElement.create(11, 2, 0),
                        InformationElement.create(14, 4, 0),
                        InformationElement.create(16, 4, 0),
                        InformationElement.create(17, 4, 0),
                        InformationElement.create(27, 16, 0),
                        InformationElement.create(28, 16, 0),
                        InformationElement.create(136, 1, 0),
                        InformationElement.create(139, 2, 0),
                        InformationElement.create(152, 8, 0),
                        InformationElement.create(153, 8, 0),
                        InformationElement.create(110, 4, 3054),
                        InformationElement.create(111, 65535, 3054),
                        InformationElement.create(120, 4, 3054),
                        InformationElement.create(121, 65535, 3054),
                        InformationElement.create(122, 4, 3054),
                        InformationElement.create(123, 65535, 3054),
                        InformationElement.create(125, 65535, 3054),
                        InformationElement.create(126, 4, 3054),
                        InformationElement.create(127, 4, 3054),
                        InformationElement.create(140, 4, 3054),
                        InformationElement.create(141, 65535, 3054),
                        InformationElement.create(142, 4, 3054),
                        InformationElement.create(143, 65535, 3054),
                        InformationElement.create(145, 65535, 3054),
                        InformationElement.create(146, 4, 3054),
                        InformationElement.create(147, 4, 3054),
                        InformationElement.create(160, 1, 3054),
                        InformationElement.create(161, 65535, 3054),
                        InformationElement.create(162, 1, 3054),
                        InformationElement.create(163, 65535, 3054),
                        InformationElement.create(178, 65535, 3054),
                        InformationElement.create(179, 65535, 3054),
                        InformationElement.create(180, 2, 3054),
                        InformationElement.create(182, 65535, 3054),
                        InformationElement.create(183, 65535, 3054),
                        InformationElement.create(184, 65535, 3054),
                        InformationElement.create(185, 65535, 3054),
                        InformationElement.create(186, 65535, 3054),
                        InformationElement.create(187, 65535, 3054),
                        InformationElement.create(188, 4, 3054),
                        InformationElement.create(189, 65535, 3054),
                        InformationElement.create(190, 65535, 3054),
                        InformationElement.create(191, 65535, 3054),
                        InformationElement.create(192, 65535, 3054)
                );

        final ImmutableList<OptionsTemplateRecord> optionsTemplateRecords = ipfixMessage.optionsTemplateRecords();
        assertThat(optionsTemplateRecords).describedAs("options template records").hasSize(1);
        assertThat(optionsTemplateRecords.get(0).scopeFields())
                .describedAs("first record's scope fields")
                .hasSize(1);
        assertThat(ipfixMessage.flows()).describedAs("Flows")
                                        .hasSize(1);
        final Flow flow = ipfixMessage.flows().get(0);
        assertThat(flow.fields()).containsExactly(
                immutableEntry("octetDeltaCount", 103L),
                immutableEntry("packetDeltaCount", 1L),
                immutableEntry("protocolIdentifier", 17L),
                immutableEntry("tcpControlBits", 0L),
                immutableEntry("sourceTransportPort", 53L),
                immutableEntry("sourceIPv4Address", "36.83.97.149"),
                immutableEntry("ingressInterface", 1L),
                immutableEntry("destinationTransportPort", 30297L),
                immutableEntry("destinationIPv4Address", "36.83.97.7"),
                immutableEntry("egressInterface", 1L),
                immutableEntry("bgpSourceAsNumber", 17974L),
                immutableEntry("bgpDestinationAsNumber", 17974L),
                immutableEntry("icmpTypeCodeIPv4", 0L),
                immutableEntry("flowEndReason", 1L),
                immutableEntry("flowStartMilliseconds", ZonedDateTime.of(2018, 9, 13, 21, 39, 13, 249_000_000, ZoneOffset.UTC)),
                immutableEntry("flowEndMilliseconds", ZonedDateTime.of(2018, 9, 13, 21, 39, 13, 249_000_000, ZoneOffset.UTC)),
                immutableEntry("l7ApplicationId", 1L),
                immutableEntry("l7ApplicationName", "domain"),
                immutableEntry("sourceIpCountryCode", "ID"),
                immutableEntry("sourceIpCountryName", "Indonesia"),
                immutableEntry("sourceIpRegionCode", "SN"),
                immutableEntry("sourceIpRegionName", "South Sulawesi"),
                immutableEntry("sourceIpCityName", "Ballaparang"),
                immutableEntry("sourceIpLatitude", -5.146399974822998),
                immutableEntry("sourceIpLongitude", 119.44129943847656),
                immutableEntry("destinationIpCountryCode", "ID"),
                immutableEntry("destinationIpCountryName", "Indonesia"),
                immutableEntry("destinationIpRegionCode", "SN"),
                immutableEntry("destinationIpRegionName", "South Sulawesi"),
                immutableEntry("destinationIpCityName", "Ballaparang"),
                immutableEntry("destinationIpLatitude", -5.146399974822998),
                immutableEntry("destinationIpLongitude", 119.44129943847656),
                immutableEntry("osDeviceId", 0L),
                immutableEntry("osDeviceName", "unknown"),
                immutableEntry("browserId", 0L),
                immutableEntry("browserName", "-"),
                immutableEntry("sslConnectionEncryptionType", "Cleartext"),
                immutableEntry("sslEncryptionCipherName", "none"),
                immutableEntry("sslEncryptionKeyLength", 0L),
                immutableEntry("userAgent", ""),
                immutableEntry("hostName", ""),
                immutableEntry("uri", ""),
                immutableEntry("dnsText", ""),
                immutableEntry("sourceAsName", "TELKOMNET-AS2-AP PT Telekomunikasi Indonesia, ID"),
                immutableEntry("destinationAsName", "TELKOMNET-AS2-AP PT Telekomunikasi Indonesia, ID"),
                immutableEntry("transactionLatency", 53L),
                immutableEntry("dnsQueryHostName", "server-2453614b.example.int."),
                immutableEntry("dnsResponseHostName", "server-2453614b.example.int."),
                immutableEntry("dnsClasses", "IN"),
                immutableEntry("threatType", ""),
                immutableEntry("threatIpv4", "0.0.0.0")
        );
    }

}
