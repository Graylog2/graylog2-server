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
import { Trans, useTranslation } from 'react-i18next';

import { ExternalLink } from 'components/common';
import FormDataContext from 'integrations/contexts/FormDataContext';
import { AdvancedOptionsContext } from 'integrations/aws/context/AdvancedOptions';
import AdditionalFields from 'integrations/aws/common/AdditionalFields';
import ValidatedInput from 'integrations/aws/common/ValidatedInput';
import { SectionTitle, SectionNote } from 'integrations/aws/common/sharedStyles';
import useProductName from 'brand-customization/useProductName';

const INPUT_PATTERN = 'https://(.*)';
const INPUT_PLACEHOLDER = 'https://vpce-1234.service.region.vpce.amazonaws.com';

const StyledSectionTitle = styled(SectionTitle)`
  margin: 12px 0 0;
`;

const StyledSectionNote = styled(SectionNote)`
  margin: 0 0 12px;
`;

type AWSCustomEndpointsProps = {
  onChange: (...args: any[]) => void;
};

const AWSCustomEndpoints = ({ onChange }: AWSCustomEndpointsProps) => {
  const { t } = useTranslation('integrations');
  const { formData } = useContext(FormDataContext);
  const productName = useProductName();
  const { isAWSCustomEndpointsVisible, setAWSCustomEndpointsVisibility } = useContext(AdvancedOptionsContext);
  const { awsEndpointCloudWatch, awsEndpointDynamoDB, awsEndpointIAM, awsEndpointKinesis } = formData;

  const handleToggle = (visible) => {
    setAWSCustomEndpointsVisibility(visible);
  };

  return (
    <AdditionalFields
      title={t('aws_custom_endpoints.title')}
      visible={isAWSCustomEndpointsVisible}
      onToggle={handleToggle}>
      <StyledSectionTitle>{t('aws_custom_endpoints.section_title', { productName })}</StyledSectionTitle>
      <StyledSectionNote>
        <Trans
          t={t}
          i18nKey="aws_custom_endpoints.note"
          components={{
            vpcEndpointsLink: (
              <ExternalLink href="https://docs.aws.amazon.com/vpc/latest/userguide/vpc-endpoints.html" />
            ),
          }}
        />
      </StyledSectionNote>

      <ValidatedInput
        id="awsEndpointCloudWatch"
        type="text"
        fieldData={awsEndpointCloudWatch}
        onChange={onChange}
        label={t('aws_custom_endpoints.cloudwatch_label')}
        placeholder={INPUT_PLACEHOLDER}
        pattern={INPUT_PATTERN}
        title={t('aws_custom_endpoints.input_message')}
      />

      <ValidatedInput
        id="awsEndpointIAM"
        type="text"
        fieldData={awsEndpointIAM}
        onChange={onChange}
        label={t('aws_custom_endpoints.iam_label')}
        placeholder={INPUT_PLACEHOLDER}
        pattern={INPUT_PATTERN}
        title={t('aws_custom_endpoints.input_message')}
      />

      <ValidatedInput
        id="awsEndpointDynamoDB"
        type="text"
        fieldData={awsEndpointDynamoDB}
        onChange={onChange}
        label={t('aws_custom_endpoints.dynamodb_label')}
        placeholder={INPUT_PLACEHOLDER}
        pattern={INPUT_PATTERN}
        title={t('aws_custom_endpoints.input_message')}
      />

      <ValidatedInput
        id="awsEndpointKinesis"
        type="text"
        fieldData={awsEndpointKinesis}
        onChange={onChange}
        label={t('aws_custom_endpoints.kinesis_label')}
        placeholder={INPUT_PLACEHOLDER}
        pattern={INPUT_PATTERN}
        title={t('aws_custom_endpoints.input_message')}
      />
    </AdditionalFields>
  );
};

export default AWSCustomEndpoints;
