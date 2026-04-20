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

import ThrottlingCheckbox from 'integrations/components/ThrottlingCheckbox';
import { Input } from 'components/bootstrap';
import FormDataContext from 'integrations/contexts/FormDataContext';
import { AdvancedOptionsContext } from 'integrations/aws/context/AdvancedOptions';
import AdditionalFields from 'integrations/aws/common/AdditionalFields';

import type { HandleSqsBatchSizeType } from './types';

type FormAdvancedOptionsProps = {
  onChange: (...args: any[]) => void;
  handleSqsMessageBatchSizeChange: HandleSqsBatchSizeType;
};

const FormAdvancedOptions = ({ onChange, handleSqsMessageBatchSizeChange }: FormAdvancedOptionsProps) => {
  const { formData } = useContext(FormDataContext);
  const { isAdvancedOptionsVisible, setAdvancedOptionsVisibility } = useContext(AdvancedOptionsContext);

  const { overrideSource, awsCloudTrailThrottleEnabled, sqsMessageBatchSize, includeFullMessageJson } = formData;

  const handleToggle = (visible) => {
    setAdvancedOptionsVisibility(visible);
  };

  const internalHandleSqsBatchSizeChange = (e) => {
    const { value } = e.target;
    onChange(e, formData);

    if (value >= 1 && value <= 10) {
      handleSqsMessageBatchSizeChange('');
    } else {
      handleSqsMessageBatchSizeChange('Please select SQS Message Batch Size between 1 - 10.');
    }
  };

  return (
    <AdditionalFields title="Advanced Options" visible={isAdvancedOptionsVisible} onToggle={handleToggle}>
      <ThrottlingCheckbox
        id="awsCloudTrailThrottleEnabled"
        defaultChecked={awsCloudTrailThrottleEnabled?.value}
        onChange={onChange}
      />

      <Input
        id="overrideSource"
        type="text"
        value={overrideSource?.value}
        onChange={onChange}
        label="Override Source (optional)"
        help="The source is set to aws-cloudtrail by default. If desired, you may override it with a custom value."
      />

      <Input
        id="sqsMessageBatchSize"
        type="number"
        min="1"
        value={sqsMessageBatchSize?.value}
        onChange={internalHandleSqsBatchSizeChange}
        label="SQS Message Batch Size"
        help="The maximum number of messages to query from SQS at a time. The maximum acceptable value is 10."
      />

      <Input
        id="includeFullMessageJson"
        type="checkbox"
        checked={includeFullMessageJson?.value}
        onChange={onChange}
        label="Include full_message_json?"
        help="Store the complete CloudTrail event as JSON in the full_message_json field?"
      />
    </AdditionalFields>
  );
};

export default FormAdvancedOptions;
