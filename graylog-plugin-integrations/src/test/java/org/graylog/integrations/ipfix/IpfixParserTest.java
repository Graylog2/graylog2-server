package org.graylog.integrations.ipfix;

import com.google.common.collect.ImmutableList;
import com.google.common.io.Resources;
import io.netty.buffer.ByteBuf;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
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
        final File tempFile = temporaryFolder.newFile("tempFile.json");
        //load the custom definition file
        String custDefStr = "{ \"enterprise_number\": 3054, \"information_elements\": [ { \"element_id\": 110, \"name\": \"l7ApplicationId\", \"data_type\": \"unsigned32\" }, { \"element_id\": 111, \"name\": \"l7ApplicationName\", \"data_type\": \"string\" }, { \"element_id\": 120, \"name\": \"sourceIpCountryCode\", \"data_type\": \"string\" }, { \"element_id\": 121, \"name\": \"sourceIpCountryName\", \"data_type\": \"string\" }, { \"element_id\": 122, \"name\": \"sourceIpRegionCode\", \"data_type\": \"string\" }, { \"element_id\": 123, \"name\": \"sourceIpRegionName\", \"data_type\": \"string\" }, { \"element_id\": 125, \"name\": \"sourceIpCityName\", \"data_type\": \"string\" }, { \"element_id\": 126, \"name\": \"sourceIpLatitude\", \"data_type\": \"float32\" }, { \"element_id\": 127, \"name\": \"sourceIpLongitude\", \"data_type\": \"float32\" }, { \"element_id\": 140, \"name\": \"destinationIpCountryCode\", \"data_type\": \"string\" }, { \"element_id\": 141, \"name\": \"destinationIpCountryName\", \"data_type\": \"string\" }, { \"element_id\": 142, \"name\": \"destinationIpRegionCode\", \"data_type\": \"string\" }, { \"element_id\": 143, \"name\": \"destinationIpRegionName\", \"data_type\": \"string\" }, { \"element_id\": 145, \"name\": \"destinationIpCityName\", \"data_type\": \"string\" }, { \"element_id\": 146, \"name\": \"destinationIpLatitude\", \"data_type\": \"float32\" }, { \"element_id\": 147, \"name\": \"destinationIpLongitude\", \"data_type\": \"float32\" }, { \"element_id\": 160, \"name\": \"osDeviceId\", \"data_type\": \"unsigned8\" }, { \"element_id\": 161, \"name\": \"osDeviceName\", \"data_type\": \"string\" }, { \"element_id\": 162, \"name\": \"browserId\", \"data_type\": \"unsigned8\" }, { \"element_id\": 163, \"name\": \"browserName\", \"data_type\": \"string\" }, { \"element_id\": 176, \"name\": \"reverseOctetDeltaCount\", \"data_type\": \"unsigned64\" }, { \"element_id\": 177, \"name\": \"reversePacketDeltaCount\", \"data_type\": \"unsigned64\" }, { \"element_id\": 178, \"name\": \"sslConnectionEncryptionType\", \"data_type\": \"string\" }, { \"element_id\": 179, \"name\": \"sslEncryptionCipherName\", \"data_type\": \"string\" }, { \"element_id\": 180, \"name\": \"sslEncryptionKeyLength\", \"data_type\": \"unsigned16\" }, { \"element_id\": 182, \"name\": \"userAgent\", \"data_type\": \"string\" }, { \"element_id\": 183, \"name\": \"hostName\", \"data_type\": \"string\" }, { \"element_id\": 184, \"name\": \"uri\", \"data_type\": \"string\" }, { \"element_id\": 185, \"name\": \"dnsText\", \"data_type\": \"string\" }, { \"element_id\": 186, \"name\": \"sourceAsName\", \"data_type\": \"string\" }, { \"element_id\": 187, \"name\": \"destinationAsName\", \"data_type\": \"string\" }, { \"element_id\": 188, \"name\": \"transactionLatency\", \"data_type\": \"unsigned32\" }, { \"element_id\": 189, \"name\": \"dnsQueryHostName\", \"data_type\": \"string\" }, { \"element_id\": 190, \"name\": \"dnsResponseHostName\", \"data_type\": \"string\" }, { \"element_id\": 191, \"name\": \"dnsClasses\", \"data_type\": \"string\" }, { \"element_id\": 192, \"name\": \"threatType\", \"data_type\": \"string\" }, { \"element_id\": 193, \"name\": \"threatIpv4\", \"data_type\": \"ipv4address\" }, { \"element_id\": 194, \"name\": \"threatIpv6\", \"data_type\": \"ipv6address\" }, { \"element_id\": 195, \"name\": \"httpSession\", \"data_type\": \"subtemplatelist\" }, { \"element_id\": 196, \"name\": \"requestTime\", \"data_type\": \"unsigned32\" }, { \"element_id\": 197, \"name\": \"dnsRecord\", \"data_type\": \"subtemplatelist\" }, { \"element_id\": 198, \"name\": \"dnsName\", \"data_type\": \"string\" }, { \"element_id\": 199, \"name\": \"dnsIpv4Address\", \"data_type\": \"ipv4address\" }, { \"element_id\": 200, \"name\": \"dnsIpv6Address\", \"data_type\": \"ipv6address\" }, { \"element_id\": 201, \"name\": \"sni\", \"data_type\": \"string\" }, { \"element_id\": 457, \"name\": \"httpStatusCode\", \"data_type\": \"unsigned16\" }, { \"element_id\": 459, \"name\": \"httpRequestMethod\", \"data_type\": \"string\" }, { \"element_id\": 462, \"name\": \"httpMessageVersion\", \"data_type\": \"string\" } ] }\n";
        // Create a temporary json file.
        Files.write(tempFile.toPath(), custDefStr.getBytes(StandardCharsets.UTF_8));


        final URL customIPFixDefURL = Paths.get(tempFile.toString()).toUri().toURL();
        InformationElementDefinitions infoElementDefs = new InformationElementDefinitions(Resources.getResource("ipfix-iana-elements.json"),
                                                                                          customIPFixDefURL);

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