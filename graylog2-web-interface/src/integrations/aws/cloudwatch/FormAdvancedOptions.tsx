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

type FormAdvancedOptionsProps = {
  onChange: (...args: any[]) => void;
};

const FormAdvancedOptions = ({ onChange }: FormAdvancedOptionsProps) => {
  const { t } = useTranslation('integrations');
  const { formData } = useContext(FormDataContext);
  const { isAdvancedOptionsVisible, setAdvancedOptionsVisibility } = useContext(AdvancedOptionsContext);

  const {
    awsCloudWatchBatchSize,
    overrideSource,
    awsCloudWatchThrottleEnabled,
    awsCloudWatchAddFlowLogPrefix,
    awsCloudWatchStoreFullMessage,
  } = formData;

  const handleToggle = (visible) => {
    setAdvancedOptionsVisibility(visible);
  };

  return (
    <AdditionalFields
      title={t('aws_cloudwatch_advanced_options.title')}
      visible={isAdvancedOptionsVisible}
      onToggle={handleToggle}>
      <ThrottlingCheckbox
        id="awsCloudWatchThrottleEnabled"
        defaultChecked={awsCloudWatchThrottleEnabled?.value}
        onChange={onChange}
      />

      <Input
        id="awsCloudWatchAddFlowLogPrefix"
        type="checkbox"
        value="enable-logprefix"
        defaultChecked={awsCloudWatchAddFlowLogPrefix && awsCloudWatchAddFlowLogPrefix.value}
        onChange={onChange}
        label={t('aws_cloudwatch_advanced_options.flow_log_prefix_label')}
        help={t('aws_cloudwatch_advanced_options.flow_log_prefix_help')}
      />

      <Input
        id="awsCloudWatchStoreFullMessage"
        type="checkbox"
        value="enable-store-full-message"
        defaultChecked={awsCloudWatchStoreFullMessage?.value}
        onChange={onChange}
        label={t('aws_cloudwatch_advanced_options.store_full_message_label')}
        help={t('aws_cloudwatch_advanced_options.store_full_message_help')}
      />

      <Input
        id="overrideSource"
        type="text"
        value={overrideSource?.value}
        onChange={onChange}
        label={t('aws_cloudwatch_advanced_options.override_source_label')}
        help={t('aws_cloudwatch_advanced_options.override_source_help')}
      />

      <Input
        id="awsCloudWatchBatchSize"
        type="number"
        value={awsCloudWatchBatchSize.value || awsCloudWatchBatchSize.defaultValue}
        onChange={onChange}
        label={t('aws_cloudwatch_advanced_options.batch_size_label')}
        help={t('aws_cloudwatch_advanced_options.batch_size_help')}
      />
    </AdditionalFields>
  );
};

export default FormAdvancedOptions;
