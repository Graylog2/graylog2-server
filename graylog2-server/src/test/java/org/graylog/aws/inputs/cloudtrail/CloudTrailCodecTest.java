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
package org.graylog.aws.inputs.cloudtrail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageFactory;
import org.graylog2.plugin.TestMessageFactory;
import org.graylog2.plugin.configuration.Configuration;
import org.graylog2.plugin.journal.RawMessage;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class CloudTrailCodecTest {
    private final MessageFactory messageFactory = new TestMessageFactory();

    private static final String STATIC_CREDENTIALS_FILE = "static_credentials.json";
    private static final String TEMPORARY_CREDENTIALS_FILE = "temporary_credentials.json";

    @Test
    public void testAdditionalEventDataField() {

        final CloudTrailCodec codec = new CloudTrailCodec(Configuration.EMPTY_CONFIGURATION,
                new ObjectMapperProvider().get(), messageFactory);

        // Decode message with error code
        final RawMessage rawMessage = new RawMessage(("{\n" +
                "\"eventVersion\": \"1.05\",\n" +
                "\"userIdentity\": {\n" +
                "\"type\": \"IAMUser\",\n" +
                "\"principalId\": \"AIDAJHGSCCCCBBBBAAAA\",\n" +
                "\"arn\": \"arn:aws:iam::1111122221111:user/some.user\",\n" +
                "\"accountId\": \"1111122221111\",\n" +
                "\"userName\": \"some.user\"" +
                "},\n" +
                "\"eventTime\": \"2020-08-19T14:12:28Z\",\n" +
                "\"eventSource\": \"signin.amazonaws.com\",\n" +
                "\"eventName\": \"ConsoleLogin\",\n" +
                "\"awsRegion\": \"us-east-1\",\n" +
                "\"sourceIPAddress\": \"127.0.0.1\",\n" +
                "\"userAgent\": \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.125 Safari/537.36\",\n" +
                "\"requestParameters\": null,\n" +
                "\"responseElements\": {\n" +
                "\"ConsoleLogin\": \"Success\"\n" +
                "},\n" +
                "\"additionalEventData\": {\n" +
                "\"LoginTo\": \"https://console.aws.amazon.com/something\",\n" +
                "\"MobileVersion\": \"No\",\n" +
                "\"MFAUsed\": \"Yes\"\n" +
                "},\n" +
                "\"eventID\": \"df38ed44-32d4-43f6-898f-5a55d260a2bb\",\n" +
                "\"eventType\": \"AwsConsoleSignIn\",\n" +
                "\"recipientAccountId\": \"1111122221111\"\n" +
                "}").getBytes(StandardCharsets.UTF_8));
        Message message = codec.decodeSafe(rawMessage).get();
        String additional_event_data = message.getField("additional_event_data").toString();

        assertTrue(additional_event_data.contains("MFAUsed=Yes"));
        assertTrue(additional_event_data.contains("MobileVersion=No"));
        assertTrue(additional_event_data.contains("LoginTo=https://console.aws.amazon.com/something"));
    }

    @Test
    public void testNoAdditionalEventDataField() {

        final CloudTrailCodec codec = new CloudTrailCodec(Configuration.EMPTY_CONFIGURATION,
                new ObjectMapperProvider().get(), messageFactory);

        final RawMessage rawMessage = new RawMessage(("{\n" +
                "\"eventVersion\": \"1.05\",\n" +
                "\"userIdentity\": {\n" +
                "\"type\": \"IAMUser\",\n" +
                "\"principalId\": \"AIDAJHGSCCCCBBBBAAAA\",\n" +
                "\"arn\": \"arn:aws:iam::1111122221111:user/some.user\",\n" +
                "\"accountId\": \"1111122221111\",\n" +
                "\"userName\": \"some.user\"" +
                "},\n" +
                "\"eventTime\": \"2020-08-19T14:12:28Z\",\n" +
                "\"eventSource\": \"signin.amazonaws.com\",\n" +
                "\"eventName\": \"ConsoleLogin\",\n" +
                "\"awsRegion\": \"us-east-1\",\n" +
                "\"sourceIPAddress\": \"127.0.0.1\",\n" +
                "\"userAgent\": \"Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/84.0.4147.125 Safari/537.36\",\n" +
                "\"requestParameters\": null,\n" +
                "\"responseElements\": {\n" +
                "\"ConsoleLogin\": \"Success\"\n" +
                "},\n" +
                "\"eventID\": \"df38ed44-32d4-43f6-898f-5a55d260a2bb\",\n" +
                "\"eventType\": \"AwsConsoleSignIn\",\n" +
                "\"recipientAccountId\": \"1111122221111\"\n" +
                "}").getBytes(StandardCharsets.UTF_8));
        Message message = codec.decodeSafe(rawMessage).get();
        assertNull(message.getField("additional_event_data"));
    }

    @Test
    public void testIssue22086WithStaticCreds() throws IOException, URISyntaxException {
        final CloudTrailCodec codec = new CloudTrailCodec(Configuration.EMPTY_CONFIGURATION,
                new ObjectMapperProvider().get(), messageFactory);

        Message message = codec.decodeSafe(getRawMessageFromFile(STATIC_CREDENTIALS_FILE)).get();
        String userName = message.getField("user_name").toString();
        assertTrue(message.getMessage().contains("Alice"));
        assertEquals("Alice", userName);
        assertEquals("IAMUser", message.getField("user_type"));
        assertEquals("AIDAJ45Q7YFFAREXAMPLE", message.getField("user_principal_id"));
        assertEquals("arn:aws:iam::123456789012:user/Alice", message.getField("user_principal_arn"));
        assertEquals("123456789012", message.getField("user_account_id"));

        assertNull(message.getField("session_issuer_user_type"));
        assertNull(message.getField("session_issuer_user_principal_id"));
        assertNull(message.getField("session_issuer_user_principal_arn"));
        assertNull(message.getField("session_issuer_user_account_id"));
    }

    @Test
    public void testIssue22086WithTempCreds() throws IOException, URISyntaxException {
        final CloudTrailCodec codec = new CloudTrailCodec(Configuration.EMPTY_CONFIGURATION,
                new ObjectMapperProvider().get(), messageFactory);

        Message message = codec.decodeSafe(getRawMessageFromFile(TEMPORARY_CREDENTIALS_FILE)).get();
        String userName = message.getField("user_name").toString();
        assertTrue(message.getMessage().contains("someTestUser"));
        assertEquals("someTestUser", userName);
        assertEquals("AssumedRole", message.getField("user_type"));
        assertEquals("AROAIDPPEZS35WEXAMPLE:AssumedRoleSessionName", message.getField("user_principal_id"));
        assertEquals("arn:aws:sts::123456789012:assumed-role/someTestUser/MySessionName", message.getField("user_principal_arn"));
        assertEquals("123456789015", message.getField("user_account_id"));

        assertEquals("Role", message.getField("session_issuer_user_type"));
        assertEquals("AROAIDPPEZS35WEXAMPLE", message.getField("session_issuer_user_principal_id"));
        assertEquals("arn:aws:iam::123456789012:role/someTestUser", message.getField("session_issuer_user_principal_arn"));
        assertEquals("123456789012", message.getField("session_issuer_user_account_id"));
    }

    @Test
    public void testFullMessageJsonDisabledByDefault() {
        final CloudTrailCodec codec = new CloudTrailCodec(Configuration.EMPTY_CONFIGURATION,
                new ObjectMapperProvider().get(), messageFactory);

        final RawMessage rawMessage = new RawMessage(("{\n" +
                "\"eventVersion\": \"1.08\",\n" +
                "\"userIdentity\": {\n" +
                "\"type\": \"IAMUser\",\n" +
                "\"principalId\": \"AIDAJ45Q7YFFAREXAMPLE\",\n" +
                "\"arn\": \"arn:aws:iam::123456789012:user/Alice\",\n" +
                "\"accountId\": \"123456789012\",\n" +
                "\"userName\": \"Alice\"" +
                "},\n" +
                "\"eventTime\": \"2024-01-15T10:30:45Z\",\n" +
                "\"eventSource\": \"s3.amazonaws.com\",\n" +
                "\"eventName\": \"PutObject\",\n" +
                "\"awsRegion\": \"us-east-1\",\n" +
                "\"sourceIPAddress\": \"192.168.1.100\",\n" +
                "\"userAgent\": \"aws-cli/2.0.0\",\n" +
                "\"requestParameters\": {\n" +
                "\"bucketName\": \"my-bucket\",\n" +
                "\"key\": \"file.pdf\"\n" +
                "},\n" +
                "\"responseElements\": null,\n" +
                "\"requestID\": \"ABC123\",\n" +
                "\"eventID\": \"a1b2c3d4\",\n" +
                "\"eventType\": \"AwsApiCall\",\n" +
                "\"recipientAccountId\": \"123456789012\"\n" +
                "}").getBytes(StandardCharsets.UTF_8));

        Message message = codec.decodeSafe(rawMessage).get();

        // full_message_json should not be present when disabled (default)
        assertNull(message.getField("full_message_json"));
    }

    @Test
    public void testFullMessageJsonEnabled() throws Exception {
        final Map<String, Object> config = new HashMap<>();
        config.put(CloudTrailCodec.CK_INCLUDE_FULL_MESSAGE_JSON, true);
        final Configuration configuration = new Configuration(config);

        final CloudTrailCodec codec = new CloudTrailCodec(configuration,
                new ObjectMapperProvider().get(), messageFactory);

        final RawMessage rawMessage = new RawMessage(("{\n" +
                "\"eventVersion\": \"1.08\",\n" +
                "\"userIdentity\": {\n" +
                "\"type\": \"IAMUser\",\n" +
                "\"principalId\": \"AIDAJ45Q7YFFAREXAMPLE\",\n" +
                "\"arn\": \"arn:aws:iam::123456789012:user/Alice\",\n" +
                "\"accountId\": \"123456789012\",\n" +
                "\"userName\": \"Alice\"" +
                "},\n" +
                "\"eventTime\": \"2024-01-15T10:30:45Z\",\n" +
                "\"eventSource\": \"s3.amazonaws.com\",\n" +
                "\"eventName\": \"PutObject\",\n" +
                "\"awsRegion\": \"us-east-1\",\n" +
                "\"sourceIPAddress\": \"192.168.1.100\",\n" +
                "\"userAgent\": \"aws-cli/2.0.0\",\n" +
                "\"requestParameters\": {\n" +
                "\"bucketName\": \"my-bucket\",\n" +
                "\"key\": \"file.pdf\"\n" +
                "},\n" +
                "\"responseElements\": null,\n" +
                "\"requestID\": \"ABC123\",\n" +
                "\"eventID\": \"a1b2c3d4\",\n" +
                "\"eventType\": \"AwsApiCall\",\n" +
                "\"recipientAccountId\": \"123456789012\"\n" +
                "}").getBytes(StandardCharsets.UTF_8));

        Message message = codec.decodeSafe(rawMessage).get();

        // full_message_json should be present when enabled
        assertNotNull(message.getField("full_message_json"));

        // Parse and verify the JSON content
        String fullMessageJson = message.getField("full_message_json").toString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(fullMessageJson);

        // Verify key fields are present in the JSON
        assertEquals("1.08", jsonNode.get("eventVersion").asText());
        assertEquals("s3.amazonaws.com", jsonNode.get("eventSource").asText());
        assertEquals("PutObject", jsonNode.get("eventName").asText());
        assertEquals("us-east-1", jsonNode.get("awsRegion").asText());
        assertEquals("192.168.1.100", jsonNode.get("sourceIPAddress").asText());

        // Verify userIdentity is present and correct
        assertNotNull(jsonNode.get("userIdentity"));
        assertEquals("IAMUser", jsonNode.get("userIdentity").get("type").asText());
        assertEquals("Alice", jsonNode.get("userIdentity").get("userName").asText());

        // Verify requestParameters are present and correct
        assertNotNull(jsonNode.get("requestParameters"));
        assertEquals("my-bucket", jsonNode.get("requestParameters").get("bucketName").asText());
        assertEquals("file.pdf", jsonNode.get("requestParameters").get("key").asText());
    }

    @Test
    public void testFullMessageJsonWithNestedRequestParameters() throws Exception {
        final Map<String, Object> config = new HashMap<>();
        config.put(CloudTrailCodec.CK_INCLUDE_FULL_MESSAGE_JSON, true);
        final Configuration configuration = new Configuration(config);

        final CloudTrailCodec codec = new CloudTrailCodec(configuration,
                new ObjectMapperProvider().get(), messageFactory);

        final RawMessage rawMessage = new RawMessage(("{\n" +
                "\"eventVersion\": \"1.08\",\n" +
                "\"userIdentity\": {\n" +
                "\"type\": \"IAMUser\",\n" +
                "\"principalId\": \"AIDAJ45Q7YFFAREXAMPLE\",\n" +
                "\"arn\": \"arn:aws:iam::123456789012:user/SecurityAdmin\",\n" +
                "\"accountId\": \"123456789012\",\n" +
                "\"userName\": \"SecurityAdmin\"" +
                "},\n" +
                "\"eventTime\": \"2024-01-15T16:10:33Z\",\n" +
                "\"eventSource\": \"s3.amazonaws.com\",\n" +
                "\"eventName\": \"PutBucketPublicAccessBlock\",\n" +
                "\"awsRegion\": \"us-east-1\",\n" +
                "\"sourceIPAddress\": \"192.168.1.200\",\n" +
                "\"userAgent\": \"console.amazonaws.com\",\n" +
                "\"requestParameters\": {\n" +
                "\"bucketName\": \"sensitive-data\",\n" +
                "\"PublicAccessBlockConfiguration\": {\n" +
                "\"BlockPublicAcls\": false,\n" +
                "\"IgnorePublicAcls\": false,\n" +
                "\"BlockPublicPolicy\": true,\n" +
                "\"RestrictPublicBuckets\": true\n" +
                "}\n" +
                "},\n" +
                "\"responseElements\": null,\n" +
                "\"requestID\": \"JKL901MNO234\",\n" +
                "\"eventID\": \"d4e5f6a7\",\n" +
                "\"eventType\": \"AwsApiCall\",\n" +
                "\"recipientAccountId\": \"123456789012\"\n" +
                "}").getBytes(StandardCharsets.UTF_8));

        Message message = codec.decodeSafe(rawMessage).get();

        // full_message_json should be present
        assertNotNull(message.getField("full_message_json"));

        // Parse and verify nested requestParameters
        String fullMessageJson = message.getField("full_message_json").toString();
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(fullMessageJson);

        // Verify nested PublicAccessBlockConfiguration
        JsonNode requestParams = jsonNode.get("requestParameters");
        assertNotNull(requestParams);
        assertEquals("sensitive-data", requestParams.get("bucketName").asText());

        JsonNode publicAccessBlock = requestParams.get("PublicAccessBlockConfiguration");
        assertNotNull(publicAccessBlock);
        assertEquals(false, publicAccessBlock.get("BlockPublicAcls").asBoolean());
        assertEquals(false, publicAccessBlock.get("IgnorePublicAcls").asBoolean());
        assertEquals(true, publicAccessBlock.get("BlockPublicPolicy").asBoolean());
        assertEquals(true, publicAccessBlock.get("RestrictPublicBuckets").asBoolean());
    }

    private RawMessage getRawMessageFromFile(String fileName) throws IOException, URISyntaxException {
        File events = new File(this.getClass().getResource(fileName).toURI());
        return new RawMessage(Files.readString(events.toPath(), StandardCharsets.UTF_8).getBytes(StandardCharsets.UTF_8));
    }

}
