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
import React from 'react';

type AgreeProps = {
  groupName: string;
  streamName: string;
};

const Agree = ({
  groupName,
  streamName,
}: AgreeProps) => (
  <>
    <p>This auto setup will create the following AWS resources. Click below to acknowledge that you understand that these resources will be created and that you are solely responsible for any associated AWS fees incurred from them. Note that all resources must be manually deleted by you if they are not needed.</p>

    <ol>
      <li>Create a Kinesis stream with <strong>1</strong> shard.</li>
      <li>Create an IAM Role and Policy to allow the specified CloudWatch group <strong>{groupName}</strong> to publish log messages to the Kinesis stream <strong>{streamName}</strong></li>
      <li>Create a CloudWatch Subscription, which publishes log messages to the Kinesis stream.</li>
    </ol>
  </>
);

export default Agree;
