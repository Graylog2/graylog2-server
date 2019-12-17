import React, { useContext } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Input } from 'components/bootstrap';

import { FormDataContext } from 'aws/context/FormData';
import { AdvancedOptionsContext } from 'aws/context/AdvancedOptions';

import AdditionalFields from 'aws/common/AdditionalFields';

const FormAdvancedOptions = ({ onChange }) => {
  const { formData } = useContext(FormDataContext);
  const { isAdvancedOptionsVisible, setAdvancedOptionsVisibility } = useContext(AdvancedOptionsContext);

  const {
    awsCloudWatchGlobalInput,
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
             label="Global Input"
             help="Should this input start on all nodes" />

      <Input id="awsCloudWatchThrottleEnabled"
             type="checkbox"
             value="enable-throttling"
             defaultChecked={awsCloudWatchThrottleEnabled && awsCloudWatchThrottleEnabled.value}
             onChange={onChange}
             label="Enable Throttling"
             help="If enabled, no new messages will be read from this input until Graylog catches up with its message load. This is typically useful for inputs reading from files or message queue systems like AMQP or Kafka. If you regularly poll an external system, e.g. via HTTP, you normally want to leave this disabled." />

      <Input id="awsCloudWatchAddFlowLogPrefix"
             type="checkbox"
             value="enable-logprefix"
             defaultChecked={awsCloudWatchAddFlowLogPrefix && awsCloudWatchAddFlowLogPrefix.value}
             onChange={onChange}
             label="Add Flow Log field name prefix"
             help='Add field with the Flow Log prefix e. g. "src_addr" -> "flow_log_src_addr".' />

      <Input id="awsCloudWatchBatchSize"
             type="number"
             value={awsCloudWatchBatchSize.value || awsCloudWatchBatchSize.defaultValue}
             onChange={onChange}
             label="Kinesis Record batch size"
             help="The number of Kinesis records to fetch at a time. Each record may be up to 1MB in size. The AWS default is 10,000. Enter a smaller value to process smaller chunks at a time." />
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
