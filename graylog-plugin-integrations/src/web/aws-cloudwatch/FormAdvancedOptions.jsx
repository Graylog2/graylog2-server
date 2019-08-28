import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Input } from 'components/bootstrap';

import { FormDataContext } from './context/FormData';
import { AdvancedOptionsContext } from './context/AdvancedOptions';

import AdditionalFields from '../common/AdditionalFields';

const FormAdvancedOptions = ({ onChange }) => {
  const { formData } = useContext(FormDataContext);
  const { isAdvancedOptionsVisible, setAdvancedOptionsVisibility } = useContext(AdvancedOptionsContext);

  const {
    awsCloudWatchGlobalInput,
    awsCloudWatchAssumeARN,
    awsCloudWatchBatchSize,
    awsCloudWatchThrottleEnabled,
    awsCloudWatchAddFlowLogPrefix,
  } = formData;

  const handleToggle = (visible) => {
    setAdvancedOptionsVisibility(visible);
  };

  return (
    <StyledAdditionalFields title="Advanced Options" visible={isAdvancedOptionsVisible} onToggle={handleToggle}>
      <Input id="awsCloudWatchGlobalInput"
             type="checkbox"
             value="global-input"
             defaultChecked={awsCloudWatchGlobalInput ? awsCloudWatchGlobalInput.value : ''}
             onChange={onChange}
             label="Global Input" />

      <Input id="awsCloudWatchThrottleEnabled"
             type="checkbox"
             value="enable-throttling"
             defaultChecked={awsCloudWatchThrottleEnabled && awsCloudWatchThrottleEnabled.value}
             onChange={onChange}
             label="Enable Throttling" />

      <Input id="awsCloudWatchAddFlowLogPrefix"
             type="checkbox"
             value="enable-throttling"
             defaultChecked={awsCloudWatchAddFlowLogPrefix && awsCloudWatchAddFlowLogPrefix.value}
             onChange={onChange}
             label="Add Flow Log field name prefix" />

      <Input id="awsCloudWatchAssumeARN"
             type="text"
             value={awsCloudWatchAssumeARN ? awsCloudWatchAssumeARN.value : ''}
             onChange={onChange}
             label="AWS assume role ARN" />

      <Input id="awsCloudWatchBatchSize"
             type="number"
             value={awsCloudWatchBatchSize.value || awsCloudWatchBatchSize.defaultValue}
             onChange={onChange}
             label="Kinesis Record batch size" />
    </StyledAdditionalFields>
  );
};

FormAdvancedOptions.propTypes = {
  onChange: PropTypes.func.isRequired,
};

const StyledAdditionalFields = styled(AdditionalFields)`
  margin: 0 0 35px;
`;

export default FormAdvancedOptions;
