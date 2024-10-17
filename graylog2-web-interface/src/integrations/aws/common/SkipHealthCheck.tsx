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
import React, { useContext } from 'react';
import styled from 'styled-components';

import FormWrap from 'integrations/aws/common/FormWrap';
import AdditionalFields from 'integrations/aws/common/AdditionalFields';
import { renderOptions } from 'integrations/aws/common/Options';
import ValidatedInput from 'integrations/aws/common/ValidatedInput';
import { KINESIS_LOG_TYPES } from 'integrations/aws/common/constants';
import { FormDataContext } from 'integrations/aws/context/FormData';

type SkipHealthCheckProps = {
  onSubmit: (...args: any[]) => void;
  onChange: (...args: any[]) => void;
};

const SkipHealthCheck = ({
  onChange,
  onSubmit,
}: SkipHealthCheckProps) => {
  const { formData } = useContext(FormDataContext);

  return (
    <AdditionalFields title="Skip Health Check">
      <StyledFormWrap onSubmit={onSubmit}
                      buttonContent="Confirm"
                      title="Choose Log Type &amp; Skip Health Check"
                      disabled={!(formData.awsCloudWatchKinesisInputType && formData.awsCloudWatchKinesisInputType.value)}
                      description={(
                        <p>If you&apos;re sure of the data contained within your new <strong>{formData.awsCloudWatchKinesisStream.value}</strong> stream, then choose your option below to skip our automated check.</p>
                      )}>

        <ValidatedInput id="awsCloudWatchKinesisInputType"
                        type="select"
                        fieldData={formData.awsCloudWatchKinesisInputType}
                        onChange={onChange}
                        label="Choose AWS Input Type"
                        required>
          {renderOptions(KINESIS_LOG_TYPES, 'Choose Log Type')}
        </ValidatedInput>
      </StyledFormWrap>
    </AdditionalFields>
  );
};

const StyledFormWrap = styled(FormWrap)`
  padding-top: 25px;
`;

export default SkipHealthCheck;
