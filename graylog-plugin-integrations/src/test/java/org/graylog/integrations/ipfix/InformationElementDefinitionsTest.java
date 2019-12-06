package org.graylog.integrations.ipfix;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.io.Resources;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class InformationElementDefinitionsTest {

    private static final Logger LOG = LoggerFactory.getLogger(InformationElementDefinitionsTest.class);
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Test
    public void buildPenToIedsMap() throws IOException {
        // Standard Definition file
        InformationElementDefinitions definitions = new InformationElementDefinitions(
                Resources.getResource("ipfix-iana-elements.json")
        );
        // Load the custom definition file
        String custDefStr = "{ \"enterprise_number\": 3054, \"information_elements\": [ { \"element_id\": 110, \"name\": \"l7ApplicationId\", \"data_type\": \"unsigned32\" }, { \"element_id\": 111, \"name\": \"l7ApplicationName\", \"data_type\": \"string\" }, { \"element_id\": 120, \"name\": \"sourceIpCountryCode\", \"data_type\": \"string\" }, { \"element_id\": 121, \"name\": \"sourceIpCountryName\", \"data_type\": \"string\" }, { \"element_id\": 122, \"name\": \"sourceIpRegionCode\", \"data_type\": \"string\" }, { \"element_id\": 123, \"name\": \"sourceIpRegionName\", \"data_type\": \"string\" }, { \"element_id\": 125, \"name\": \"sourceIpCityName\", \"data_type\": \"string\" }, { \"element_id\": 126, \"name\": \"sourceIpLatitude\", \"data_type\": \"float32\" }, { \"element_id\": 127, \"name\": \"sourceIpLongitude\", \"data_type\": \"float32\" }, { \"element_id\": 140, \"name\": \"destinationIpCountryCode\", \"data_type\": \"string\" }, { \"element_id\": 141, \"name\": \"destinationIpCountryName\", \"data_type\": \"string\" }, { \"element_id\": 142, \"name\": \"destinationIpRegionCode\", \"data_type\": \"string\" }, { \"element_id\": 143, \"name\": \"destinationIpRegionName\", \"data_type\": \"string\" }, { \"element_id\": 145, \"name\": \"destinationIpCityName\", \"data_type\": \"string\" }, { \"element_id\": 146, \"name\": \"destinationIpLatitude\", \"data_type\": \"float32\" }, { \"element_id\": 147, \"name\": \"destinationIpLongitude\", \"data_type\": \"float32\" }, { \"element_id\": 160, \"name\": \"osDeviceId\", \"data_type\": \"unsigned8\" }, { \"element_id\": 161, \"name\": \"osDeviceName\", \"data_type\": \"string\" }, { \"element_id\": 162, \"name\": \"browserId\", \"data_type\": \"unsigned8\" }, { \"element_id\": 163, \"name\": \"browserName\", \"data_type\": \"string\" }, { \"element_id\": 176, \"name\": \"reverseOctetDeltaCount\", \"data_type\": \"unsigned64\" }, { \"element_id\": 177, \"name\": \"reversePacketDeltaCount\", \"data_type\": \"unsigned64\" }, { \"element_id\": 178, \"name\": \"sslConnectionEncryptionType\", \"data_type\": \"string\" }, { \"element_id\": 179, \"name\": \"sslEncryptionCipherName\", \"data_type\": \"string\" }, { \"element_id\": 180, \"name\": \"sslEncryptionKeyLength\", \"data_type\": \"unsigned16\" }, { \"element_id\": 182, \"name\": \"userAgent\", \"data_type\": \"string\" }, { \"element_id\": 183, \"name\": \"hostName\", \"data_type\": \"string\" }, { \"element_id\": 184, \"name\": \"uri\", \"data_type\": \"string\" }, { \"element_id\": 185, \"name\": \"dnsText\", \"data_type\": \"string\" }, { \"element_id\": 186, \"name\": \"sourceAsName\", \"data_type\": \"string\" }, { \"element_id\": 187, \"name\": \"destinationAsName\", \"data_type\": \"string\" }, { \"element_id\": 188, \"name\": \"transactionLatency\", \"data_type\": \"unsigned32\" }, { \"element_id\": 189, \"name\": \"dnsQueryHostName\", \"data_type\": \"string\" }, { \"element_id\": 190, \"name\": \"dnsResponseHostName\", \"data_type\": \"string\" }, { \"element_id\": 191, \"name\": \"dnsClasses\", \"data_type\": \"string\" }, { \"element_id\": 192, \"name\": \"threatType\", \"data_type\": \"string\" }, { \"element_id\": 193, \"name\": \"threatIpv4\", \"data_type\": \"ipv4address\" }, { \"element_id\": 194, \"name\": \"threatIpv6\", \"data_type\": \"ipv6address\" }, { \"element_id\": 195, \"name\": \"httpSession\", \"data_type\": \"subtemplatelist\" }, { \"element_id\": 196, \"name\": \"requestTime\", \"data_type\": \"unsigned32\" }, { \"element_id\": 197, \"name\": \"dnsRecord\", \"data_type\": \"subtemplatelist\" }, { \"element_id\": 198, \"name\": \"dnsName\", \"data_type\": \"string\" }, { \"element_id\": 199, \"name\": \"dnsIpv4Address\", \"data_type\": \"ipv4address\" }, { \"element_id\": 200, \"name\": \"dnsIpv6Address\", \"data_type\": \"ipv6address\" }, { \"element_id\": 201, \"name\": \"sni\", \"data_type\": \"string\" }, { \"element_id\": 457, \"name\": \"httpStatusCode\", \"data_type\": \"unsigned16\" }, { \"element_id\": 459, \"name\": \"httpRequestMethod\", \"data_type\": \"string\" }, { \"element_id\": 462, \"name\": \"httpMessageVersion\", \"data_type\": \"string\" } ] }\n";
        // Create a temporary json file.
        final File tempFile = tempFolder.newFile("tempFile.json");
        // Write customDefString to it.
        FileUtils.writeStringToFile(tempFile, custDefStr, StandardCharsets.UTF_8);
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode custDefJsonNode = objectMapper.readTree(tempFile);
        Map map = definitions.buildPenToIedsMap(custDefJsonNode);
        // enterprise number holds the key
        assertEquals(map.size(), 2);
    }

    @Test
    public void testBuildPenToIedsMap() {
    }
}