import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import FormWrap from 'aws/common/FormWrap';
import AdditionalFields from 'aws/common/AdditionalFields';
import { renderOptions } from 'aws/common/Options';
import ValidatedInput from 'aws/common/ValidatedInput';
import { KINESIS_LOG_TYPES } from 'aws/common/constants';

import { FormDataContext } from 'aws/context/FormData';

const SkipHealthCheck = ({ onChange, onSubmit }) => {
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

SkipHealthCheck.propTypes = {
  onSubmit: PropTypes.func.isRequired,
  onChange: PropTypes.func.isRequired,
};

const StyledFormWrap = styled(FormWrap)`
  padding-top: 25px;
`;

export default SkipHealthCheck;
