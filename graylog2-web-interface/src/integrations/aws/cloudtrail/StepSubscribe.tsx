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
import React, { useContext, useState } from 'react';
import { useTranslation } from 'react-i18next';

import FormDataContext from 'integrations/contexts/FormDataContext';
import { Input } from 'components/bootstrap';

import FormAdvancedOptions from './FormAdvancedOptions';
import type { ErrorMessageType, HandleFieldUpdateType, HandleSubmitType } from './types';

import FormWrap from '../common/FormWrap';

interface Props {
  onSubmit: HandleSubmitType;
  onChange: HandleFieldUpdateType;
}

const StepSubscribe = ({ onSubmit, onChange }: Props) => {
  const { t } = useTranslation('integrations');
  const { formData } = useContext(FormDataContext);
  const [formError, setFormError] = useState<ErrorMessageType>(null);

  const { pollingInterval, sqsMessageBatchSize } = formData;

  const handleSubmit = () => {
    // Check if there's already a validation error (e.g., from batch size)
    if (formError) {
      return;
    }

    // Validate polling interval
    if (pollingInterval.value < 1) {
      setFormError({
        full_message: t('aws_cloudtrail_step_subscribe.polling_interval_error_full'),
        nice_message: t('aws_cloudtrail_step_subscribe.polling_interval_error'),
      });

      return;
    }

    // Validate batch size if it has a value
    if (sqsMessageBatchSize?.value && (sqsMessageBatchSize.value < 1 || sqsMessageBatchSize.value > 10)) {
      setFormError({
        full_message: t('aws_cloudtrail_step_subscribe.batch_size_error_full'),
        nice_message: t('aws_cloudtrail_step_subscribe.batch_size_error'),
      });

      return;
    }

    setFormError(null);
    onSubmit();
  };

  const handleSqsMessageBatchSizeChange = (errorMessage) => {
    if (errorMessage) {
      setFormError({
        full_message: errorMessage,
        nice_message: errorMessage,
      });
    } else {
      setFormError(null); // Reset formError if no error message
    }
  };

  return (
    <FormWrap
      onSubmit={handleSubmit}
      buttonContent={t('aws_cloudtrail_step_subscribe.proceed_button')}
      title=""
      error={formError}
      description="">
      <Input
        id="pollingInterval"
        name="pollingInterval"
        type="number"
        value={pollingInterval.value || pollingInterval.defaultValue || ''}
        min="1"
        max="60"
        step="1"
        onChange={onChange}
        required
        help={t('aws_cloudtrail_step_subscribe.polling_interval_help')}
        label={t('aws_cloudtrail_step_subscribe.polling_interval_label')}
      />

      <FormAdvancedOptions onChange={onChange} handleSqsMessageBatchSizeChange={handleSqsMessageBatchSizeChange} />
    </FormWrap>
  );
};

export default StepSubscribe;
