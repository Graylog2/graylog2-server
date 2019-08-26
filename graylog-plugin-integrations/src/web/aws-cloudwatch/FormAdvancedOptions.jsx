import React, { useContext } from 'react';
import styled from 'styled-components';
import PropTypes from 'prop-types';
import { Input } from 'components/bootstrap';

import { FormDataContext } from './context/FormData';
import { AdvancedOptionsContext } from './context/AdvancedOptions';

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

  const handleToggle = () => {
    setAdvancedOptionsVisibility(!isAdvancedOptionsVisible);
  };

  return (
    <>
      <ToggleAdvancedOptions onClick={handleToggle} type="button">
          Advanced Options <i className="fa fa-angle-right fa-sm" />
      </ToggleAdvancedOptions>

      <AdvancedOptionsContent visible={isAdvancedOptionsVisible}>
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
      </AdvancedOptionsContent>
    </>
  );
};

FormAdvancedOptions.propTypes = {
  onChange: PropTypes.func.isRequired,
};

const AdvancedOptionsContent = styled.div`
  display: ${props => (props.visible ? 'block' : 'none')};
`;

const ToggleAdvancedOptions = styled.button`
  border: 0;
  color: #16ace3;
  font-size: 14px;
  display: block;
  margin: 0 0 35px;

  :hover {
    color: #5e123b;
    text-decoration: underline;
  }
`;

export default FormAdvancedOptions;
