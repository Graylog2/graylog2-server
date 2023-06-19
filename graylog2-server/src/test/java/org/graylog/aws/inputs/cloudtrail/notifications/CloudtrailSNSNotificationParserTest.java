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
package org.graylog.aws.inputs.cloudtrail.notifications;

import com.amazonaws.services.sqs.model.Message;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CloudtrailSNSNotificationParserTest {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    @Test
    public void testParse() throws Exception {
        final Message message = new Message()
                .withBody("{\n" +
                        "  \"Type\" : \"Notification\",\n" +
                        "  \"MessageId\" : \"55508fe9-870b-590c-960f-c34960b669f0\",\n" +
                        "  \"TopicArn\" : \"arn:aws:sns:eu-west-1:459220251735:cloudtrail-write\",\n" +
                        "  \"Message\" : \"{\\\"s3Bucket\\\":\\\"cloudtrailbucket\\\",\\\"s3ObjectKey\\\":[\\\"example/AWSLogs/459220251735/CloudTrail/eu-west-1/2014/09/27/459220251735_CloudTrail_eu-west-1_20140927T1625Z_UPwzr7ft2mf0Q1SS.json.gz\\\"]}\",\n" +
                        "  \"Timestamp\" : \"2014-09-27T16:27:41.258Z\",\n" +
                        "  \"SignatureVersion\" : \"1\",\n" +
                        "  \"Signature\" : \"O05joR97NvGHqMJQwsSNXzeSHrtbLqbRcqsXB7xmqARyaCGXjaVh2duwTUL93s4YvoNENnOEMzkILKI5PwmQQPha5/cmj6FSjblwRMMga6Xzf6cMnurT9TphQO7z35foHG49IejW05IkzIwD/DW0GvafJLah+fQI3EFySnShzXLFESGQuumdS8bxnM5r96ne8t+MEAHfBCVyQ/QrduO9tTtfXAz6OeWg1IEwV3TeZ5c5SS5vRxxhsD4hOJSmXAUM99CeQfcG9s7saBcvyyGPZrhPEh8S1uhiTmLvr6h1voM9vgiCbCCUujExvg+bnqsXWTZBmnatF1iOyxFfYcZ6kw==\",\n" +
                        "  \"SigningCertURL\" : \"https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-d6d679a1d18e95c2f9ffcf11f4f9e198.pem\",\n" +
                        "  \"UnsubscribeURL\" : \"https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:459220251735:cloudtrail-write:9a3a4e76-4173-4c8c-b488-0126315ba643\"\n" +
                        "}");

        CloudtrailSNSNotificationParser parser = new CloudtrailSNSNotificationParser(objectMapper);

        List<CloudtrailSNSNotification> notifications = parser.parse(message);
        assertEquals(1, notifications.size());

        CloudtrailSNSNotification notification = notifications.get(0);

        assertEquals(notification.getS3Bucket(), "cloudtrailbucket");
        assertEquals(notification.getS3ObjectKey(), "example/AWSLogs/459220251735/CloudTrail/eu-west-1/2014/09/27/459220251735_CloudTrail_eu-west-1_20140927T1625Z_UPwzr7ft2mf0Q1SS.json.gz");
    }

    @Test
    public void testParseWithUnknownProperty() throws Exception {
        final Message message = new Message()
                .withBody("{\"Message\" : \"{\\\"Foobar\\\" : \\\"Quux\\\",\\\"s3Bucket\\\":\\\"cloudtrailbucket\\\",\\\"s3ObjectKey\\\":[\\\"example/AWSLogs/459220251735/CloudTrail/eu-west-1/2014/09/27/459220251735_CloudTrail_eu-west-1_20140927T1625Z_UPwzr7ft2mf0Q1SS.json.gz\\\"]}\"}");

        CloudtrailSNSNotificationParser parser = new CloudtrailSNSNotificationParser(objectMapper);

        List<CloudtrailSNSNotification> notifications = parser.parse(message);
        assertEquals(1, notifications.size());

        CloudtrailSNSNotification notification = notifications.get(0);

        assertEquals(notification.getS3Bucket(), "cloudtrailbucket");
        assertEquals(notification.getS3ObjectKey(), "example/AWSLogs/459220251735/CloudTrail/eu-west-1/2014/09/27/459220251735_CloudTrail_eu-west-1_20140927T1625Z_UPwzr7ft2mf0Q1SS.json.gz");
    }

    @Test
    public void issue_44() throws Exception {
        // https://github.com/Graylog2/graylog-plugin-aws/issues/44
        final Message message = new Message()
                .withBody("{\n" +
                        "  \"Type\" : \"Notification\",\n" +
                        "  \"MessageId\" : \"5b0a73e6-a4f8-11e7-8dfb-8f76310a10a8\",\n" +
                        "  \"TopicArn\" : \"arn:aws:sns:eu-west-1:123456789012:cloudtrail-log-write\",\n" +
                        "  \"Subject\" : \"[AWS Config:eu-west-1] AWS::RDS::DBSnapshot rds:instance-2017-09-03-23-11 Dele...\",\n" +
                        "  \"Message\" : \"{\\\"configurationItemDiff\\\":{\\\"changedProperties\\\":{\\\"Relationships.0\\\":{\\\"previousValue\\\":{\\\"resourceId\\\":\\\"vpc-12345678\\\",\\\"resourceName\\\":null,\\\"resourceType\\\":\\\"AWS::EC2::VPC\\\",\\\"name\\\":\\\"Is associated with Vpc\\\"},\\\"updatedValue\\\":null,\\\"changeType\\\":\\\"DELETE\\\"},\\\"SupplementaryConfiguration.Tags\\\":{\\\"previousValue\\\":[],\\\"updatedValue\\\":null,\\\"changeType\\\":\\\"DELETE\\\"},\\\"SupplementaryConfiguration.DBSnapshotAttributes\\\":{\\\"previousValue\\\":[{\\\"attributeName\\\":\\\"restore\\\",\\\"attributeValues\\\":[]}],\\\"updatedValue\\\":null,\\\"changeType\\\":\\\"DELETE\\\"},\\\"Configuration\\\":{\\\"previousValue\\\":{\\\"dBSnapshotIdentifier\\\":\\\"rds:instance-2017-09-03-23-11\\\",\\\"dBInstanceIdentifier\\\":\\\"instance\\\",\\\"snapshotCreateTime\\\":\\\"2017-09-03T23:11:38.218Z\\\",\\\"engine\\\":\\\"mysql\\\",\\\"allocatedStorage\\\":200,\\\"status\\\":\\\"available\\\",\\\"port\\\":3306,\\\"availabilityZone\\\":\\\"eu-west-1b\\\",\\\"vpcId\\\":\\\"vpc-12345678\\\",\\\"instanceCreateTime\\\":\\\"2015-04-09T07:08:07.476Z\\\",\\\"masterUsername\\\":\\\"root\\\",\\\"engineVersion\\\":\\\"5.6.34\\\",\\\"licenseModel\\\":\\\"general-public-license\\\",\\\"snapshotType\\\":\\\"automated\\\",\\\"iops\\\":null,\\\"optionGroupName\\\":\\\"default:mysql-5-6\\\",\\\"percentProgress\\\":100,\\\"sourceRegion\\\":null,\\\"sourceDBSnapshotIdentifier\\\":null,\\\"storageType\\\":\\\"standard\\\",\\\"tdeCredentialArn\\\":null,\\\"encrypted\\\":false,\\\"kmsKeyId\\\":null,\\\"dBSnapshotArn\\\":\\\"arn:aws:rds:eu-west-1:123456789012:snapshot:rds:instance-2017-09-03-23-11\\\",\\\"timezone\\\":null,\\\"iAMDatabaseAuthenticationEnabled\\\":false},\\\"updatedValue\\\":null,\\\"changeType\\\":\\\"DELETE\\\"}},\\\"changeType\\\":\\\"DELETE\\\"},\\\"configurationItem\\\":{\\\"relatedEvents\\\":[],\\\"relationships\\\":[],\\\"configuration\\\":null,\\\"supplementaryConfiguration\\\":{},\\\"tags\\\":{},\\\"configurationItemVersion\\\":\\\"1.2\\\",\\\"configurationItemCaptureTime\\\":\\\"2017-09-28T19:54:47.815Z\\\",\\\"configurationStateId\\\":1234567890123,\\\"awsAccountId\\\":\\\"123456789012\\\",\\\"configurationItemStatus\\\":\\\"ResourceDeleted\\\",\\\"resourceType\\\":\\\"AWS::RDS::DBSnapshot\\\",\\\"resourceId\\\":\\\"rds:instance-2017-09-03-23-11\\\",\\\"resourceName\\\":\\\"rds:instance-2017-09-03-23-11\\\",\\\"ARN\\\":\\\"arn:aws:rds:eu-west-1:123456789012:snapshot:rds:instance-2017-09-03-23-11\\\",\\\"awsRegion\\\":\\\"eu-west-1\\\",\\\"availabilityZone\\\":null,\\\"configurationStateMd5Hash\\\":\\\"b026324c6904b2a9cb4b88d6d61c81d1\\\",\\\"resourceCreationTime\\\":null},\\\"notificationCreationTime\\\":\\\"2017-09-28T19:54:48.311Z\\\",\\\"messageType\\\":\\\"ConfigurationItemChangeNotification\\\",\\\"recordVersion\\\":\\\"1.2\\\"}\",\n" +
                        "  \"Timestamp\" : \"2017-09-28T19:54:58.543Z\",\n" +
                        "  \"SignatureVersion\" : \"1\",\n" +
                        "  \"Signature\" : \"...\",\n" +
                        "  \"SigningCertURL\" : \"https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-....pem\",\n" +
                        "  \"UnsubscribeURL\" : \"https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:123456789012:cloudtrail-log-write:5b0a73e6-a4f8-11e7-8dfb-8f76310a10a8\"\n" +
                        "}");

        CloudtrailSNSNotificationParser parser = new CloudtrailSNSNotificationParser(objectMapper);

        List<CloudtrailSNSNotification> notifications = parser.parse(message);
        assertTrue(notifications.isEmpty());
    }

    @Test
    public void testParseWithTwoS3Objects() throws Exception {
        final Message doubleMessage = new Message()
                .withBody("{\n" +
                        "  \"Type\" : \"Notification\",\n" +
                        "  \"MessageId\" : \"11a04c4a-094e-5395-b297-00eaefda2893\",\n" +
                        "  \"TopicArn\" : \"arn:aws:sns:eu-west-1:459220251735:cloudtrail-write\",\n" +
                        "  \"Message\" : \"{\\\"s3Bucket\\\":\\\"cloudtrailbucket\\\",\\\"s3ObjectKey\\\":[\\\"example/AWSLogs/459220251735/CloudTrail/eu-west-1/2014/09/27/459220251735_CloudTrail_eu-west-1_20140927T1620Z_Nk2SdmlEzA0gDpPr.json.gz\\\", \\\"example/AWSLogs/459220251735/CloudTrail/eu-west-1/2014/09/27/459220251999_CloudTrail2_eu-west-1_20140927T1620Z_Nk2SdmlEzA0gDpPr.json.gz\\\"]}\",\n" +
                        "  \"Timestamp\" : \"2014-09-27T16:22:44.011Z\",\n" +
                        "  \"SignatureVersion\" : \"1\",\n" +
                        "  \"Signature\" : \"q9xmJZ8nJR5iaAYMLN3M8v9HyLbUqbLjGGFlmmvIK9UDQiQO0wmvlYeo5/lQqvANW/v+NVXZxxOoWx06p6Rv5BwXIa2ASVh7RlXc2y+U2pQgLaQlJ671cA33iBi/iH1al/7lTLrlIkUb9m2gAdEyulbhZfBfAQOm7GN1PHR/nW+CtT61g4KvMSonNzj23jglLTb0r6pxxQ5EmXz6Jo5DOsbXvuFt0BSyVP/8xRXT1ap0S7BuUOstz8+FMqdUyOQSR9RA9r61yUsJ4nnq0KfK5/1gjTTDPmE4OkGvk6AuV9YTME7FWTY/wU4LPg5/+g/rUo2UDGrxnGoJ3OUW5yrtyQ==\",\n" +
                        "  \"SigningCertURL\" : \"https://sns.eu-west-1.amazonaws.com/SimpleNotificationService-d6d679a1d18e95c2f9ffcf11f4f9e198.pem\",\n" +
                        "  \"UnsubscribeURL\" : \"https://sns.eu-west-1.amazonaws.com/?Action=Unsubscribe&SubscriptionArn=arn:aws:sns:eu-west-1:459220251735:cloudtrail-write:9a3a4e76-4173-4c8c-b488-0126315ba643\"\n" +
                        "}");

        CloudtrailSNSNotificationParser parser = new CloudtrailSNSNotificationParser(objectMapper);

        List<CloudtrailSNSNotification> notifications = parser.parse(doubleMessage);
        assertEquals(2, notifications.size());

        CloudtrailSNSNotification notification1 = notifications.get(0);
        CloudtrailSNSNotification notification2 = notifications.get(1);

        assertEquals(notification1.getS3Bucket(), "cloudtrailbucket");
        assertEquals(notification1.getS3ObjectKey(), "example/AWSLogs/459220251735/CloudTrail/eu-west-1/2014/09/27/459220251735_CloudTrail_eu-west-1_20140927T1620Z_Nk2SdmlEzA0gDpPr.json.gz");

        assertEquals(notification2.getS3Bucket(), "cloudtrailbucket");
        assertEquals(notification2.getS3ObjectKey(), "example/AWSLogs/459220251735/CloudTrail/eu-west-1/2014/09/27/459220251999_CloudTrail2_eu-west-1_20140927T1620Z_Nk2SdmlEzA0gDpPr.json.gz");
    }
}
