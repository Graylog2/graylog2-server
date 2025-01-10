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

import { Input } from 'components/bootstrap';
import { FormDataContext } from 'integrations/aws/context/FormData';
import { AdvancedOptionsContext } from 'integrations/aws/context/AdvancedOptions';
import AdditionalFields from 'integrations/aws/common/AdditionalFields';

type FormAdvancedOptionsProps = {
  onChange: (...args: any[]) => void;
};

const FormAdvancedOptions = ({
  onChange,
}: FormAdvancedOptionsProps) => {
  const { formData } = useContext(FormDataContext);
  const { isAdvancedOptionsVisible, setAdvancedOptionsVisibility } = useContext(AdvancedOptionsContext);

  const {
    awsCloudWatchBatchSize,
    awsCloudWatchThrottleEnabled,
    awsCloudWatchAddFlowLogPrefix,
  } = formData;

  const handleToggle = (visible) => {
    setAdvancedOptionsVisibility(visible);
  };

  return (
    <StyledAdditionalFields title="Advanced Options" visible={isAdvancedOptionsVisible} onToggle={handleToggle}>
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

const StyledAdditionalFields = styled(AdditionalFields)`
margin: 0 0 35px;
`;

export default FormAdvancedOptions;
