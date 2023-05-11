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

public class CloudtrailSNSNotification {
    private final String s3Bucket;
    private final String s3ObjectKey;
    private final String receiptHandle;

    protected CloudtrailSNSNotification(String receiptHandle, String s3Bucket, String s3ObjectKey) {
        this.receiptHandle = receiptHandle;
        this.s3Bucket = s3Bucket;
        this.s3ObjectKey = s3ObjectKey;
    }

    public String getReceiptHandle() {
        return receiptHandle;
    }

    public String getS3Bucket() {
        return s3Bucket;
    }

    public String getS3ObjectKey() {
        return s3ObjectKey;
    }

}
