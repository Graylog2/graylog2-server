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

import { Input } from 'components/bootstrap';

type ARNProps = {
  awsARN?: {
    value?: string;
  };
  onChange: (...args: any[]) => void;
};

const ARN = ({
  awsARN = {
    value: '',
  },

  onChange,
}: ARNProps) => (
  <Input id="awsCloudWatchAssumeARN"
         type="text"
         value={awsARN.value}
         onChange={onChange}
         label="AWS Assume Role (ARN)"
         help="Amazon Resource Name with required cross account permission"
         placeholder="arn:aws:sts::123456789012:assumed-role/some-role"
         maxLength={2048} />
);

export default ARN;
