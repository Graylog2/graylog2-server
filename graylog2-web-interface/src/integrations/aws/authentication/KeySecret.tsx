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
import styled from 'styled-components';

import ValidatedInput from 'integrations/aws/common/ValidatedInput';
import MaskedInput from 'integrations/aws/common/MaskedInput';

type KeySecretProps = {
  onChange: (...args: any[]) => void;
  awsKey?: any;
  awsSecret?: any;
};

const KeySecret = ({
  onChange,
  awsKey,
  awsSecret,
}: KeySecretProps) => (
  <>
    <ValidatedInput id="awsCloudWatchAwsKey"
                    type="text"
                    label="AWS Access Key"
                    placeholder="AK****************"
                    onChange={onChange}
                    fieldData={awsKey}
                    autoComplete="off"
                    maxLength={512}
                    help='Your AWS Key should be a 20-character long, alphanumeric string that starts with the letters "AK".'
                    required />

    <StyledMaskedInput id="awsCloudWatchAwsSecret"
                       label="AWS Secret Key"
                       placeholder="***********"
                       onChange={onChange}
                       fieldData={awsSecret}
                       autoComplete="off"
                       maxLength={512}
                       help="Your AWS Secret is usually a 40-character long, base-64 encoded string."
                       required />
  </>
);

const StyledMaskedInput = styled(MaskedInput)`
margin-bottom: 0;
`;

export default KeySecret;
