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

import { ExternalLink } from 'components/common';
import { FormDataContext } from 'integrations/aws/context/FormData';
import { AdvancedOptionsContext } from 'integrations/aws/context/AdvancedOptions';
import AdditionalFields from 'integrations/aws/common/AdditionalFields';
import ValidatedInput from 'integrations/aws/common/ValidatedInput';
import { SectionTitle, SectionNote } from 'integrations/aws/common/sharedStyles';

const INPUT_PATTERN = 'https://(.*)';
const INPUT_MESSAGE = "Domain should begin with 'https://'";
const INPUT_PLACEHOLDER = 'https://vpce-1234.service.region.vpce.amazonaws.com';

const StyledAdditionalFields = styled(AdditionalFields)`
margin: 0 0 35px;
`;

const StyledSectionTitle = styled(SectionTitle)`
margin: 12px 0 0;
`;

const StyledSectionNote = styled(SectionNote)`
margin: 0 0 12px;
`;

type AWSCustomEndpointsProps = {
  onChange: (...args: any[]) => void;
};

const AWSCustomEndpoints = ({
  onChange,
}: AWSCustomEndpointsProps) => {
  const { formData } = useContext(FormDataContext);
  const { isAWSCustomEndpointsVisible, setAWSCustomEndpointsVisibility } = useContext(AdvancedOptionsContext);
  const {
    awsEndpointCloudWatch,
    awsEndpointDynamoDB,
    awsEndpointIAM,
    awsEndpointKinesis,
  } = formData;

  const handleToggle = (visible) => {
    setAWSCustomEndpointsVisibility(visible);
  };

  return (
    <StyledAdditionalFields title="Optional AWS VPC Endpoints"
                            visible={isAWSCustomEndpointsVisible}
                            onToggle={handleToggle}>

      <StyledSectionTitle>Overrides the default AWS API endpoint URL that Graylog communicates with.</StyledSectionTitle>
      <StyledSectionNote>Only specify these if you are using <ExternalLink href="https://docs.aws.amazon.com/vpc/latest/userguide/vpc-endpoints.html">VPC Endpoints</ExternalLink> for AWS services.</StyledSectionNote>

      <ValidatedInput id="awsEndpointCloudWatch"
                      type="text"
                      fieldData={awsEndpointCloudWatch}
                      onChange={onChange}
                      label="CloudWatch API Endpoint Override"
                      placeholder={INPUT_PLACEHOLDER}
                      pattern={INPUT_PATTERN}
                      title={INPUT_MESSAGE} />

      <ValidatedInput id="awsEndpointIAM"
                      type="text"
                      fieldData={awsEndpointIAM}
                      onChange={onChange}
                      label="IAM API Endpoint Override"
                      placeholder={INPUT_PLACEHOLDER}
                      pattern={INPUT_PATTERN}
                      title={INPUT_MESSAGE} />

      <ValidatedInput id="awsEndpointDynamoDB"
                      type="text"
                      fieldData={awsEndpointDynamoDB}
                      onChange={onChange}
                      label="DynamoDB API Endpoint Override"
                      placeholder={INPUT_PLACEHOLDER}
                      pattern={INPUT_PATTERN}
                      title={INPUT_MESSAGE} />

      <ValidatedInput id="awsEndpointKinesis"
                      type="text"
                      fieldData={awsEndpointKinesis}
                      onChange={onChange}
                      label="Kinesis API Endpoint Override"
                      placeholder={INPUT_PLACEHOLDER}
                      pattern={INPUT_PATTERN}
                      title={INPUT_MESSAGE} />
    </StyledAdditionalFields>
  );
};

export default AWSCustomEndpoints;
