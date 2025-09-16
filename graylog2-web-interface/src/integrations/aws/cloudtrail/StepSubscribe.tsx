import React, { useContext, useState } from 'react';
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
  const { formData } = useContext(FormDataContext);
  const [formError, setFormError] = useState<ErrorMessageType>(null);

  const { pollingInterval } = formData;

  const handleSubmit = () => {
    if (pollingInterval.value >= 1) {
      setFormError(null);
      onSubmit();
    } else {
      setFormError({
        full_message: 'Please provide valid polling interval',
        nice_message: 'Minimum allowable polling interval is 1 minute.',
      });
    }
  };

  return (
    <FormWrap onSubmit={handleSubmit} buttonContent="Proceed" title="" error={formError} description="">
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
        help="Determines how often (in minutes) new data will be checked for in AWS CloudTrail. The smallest allowable interval is 1 minute."
        label="Polling Interval"
      />

      <FormAdvancedOptions onChange={onChange} />
    </FormWrap>
  );
};

export default StepSubscribe;
