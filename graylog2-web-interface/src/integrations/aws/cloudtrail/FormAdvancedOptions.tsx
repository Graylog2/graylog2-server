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
import { useTranslation } from 'react-i18next';

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
  const { t } = useTranslation('integrations');
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
      handleSqsMessageBatchSizeChange(t('aws_cloudtrail_advanced_options.sqs_batch_size_error'));
    }
  };

  return (
    <AdditionalFields
      title={t('aws_cloudtrail_advanced_options.title')}
      visible={isAdvancedOptionsVisible}
      onToggle={handleToggle}>
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
        label={t('aws_cloudtrail_advanced_options.override_source_label')}
        help={t('aws_cloudtrail_advanced_options.override_source_help')}
      />

      <Input
        id="sqsMessageBatchSize"
        type="number"
        min="1"
        value={sqsMessageBatchSize?.value}
        onChange={internalHandleSqsBatchSizeChange}
        label={t('aws_cloudtrail_advanced_options.sqs_batch_size_label')}
        help={t('aws_cloudtrail_advanced_options.sqs_batch_size_help')}
      />

      <Input
        id="includeFullMessageJson"
        type="checkbox"
        checked={includeFullMessageJson?.value}
        onChange={onChange}
        label={t('aws_cloudtrail_advanced_options.include_full_message_json_label')}
        help={t('aws_cloudtrail_advanced_options.include_full_message_json_help')}
      />
    </AdditionalFields>
  );
};

export default FormAdvancedOptions;
