import React, { useContext, useState } from "react";

import { Input } from 'components/bootstrap';
import FormAdvancedOptions from './FormAdvancedOptions';

import FormWrap from '../common/FormWrap';
import { FormDataContext } from '../common/context/FormData';
import type {
  ErrorMessageType,
  FormDataContextType,
  HandleFieldUpdateType,
  HandleSubmitType,
} from '../common/utils/types';
import ValidatedInput from "../common/ValidatedInput";
import { renderOptions } from "../common/Options";


interface Props {
  onSubmit: HandleSubmitType;
  onChange: HandleFieldUpdateType;
}

const fieldTypes = [{ label: 'Timestamp', value: 'Timestamp' },
{ label: 'increment', value: 'increment' }];
const StepSubscribe: React.FC<Props> = ({ onSubmit, onChange }) => {
  const { formData } = useContext<FormDataContextType>(FormDataContext);
  const [formError, setFormError] = useState<ErrorMessageType>(null);
  const { pollingInterval } = formData;


  const handleSubmit = () => {
    onSubmit();
    if (pollingInterval.value >= 5 && pollingInterval.value <= 10000) {
      setFormError(null);
      onSubmit();
    }
    else {
      setFormError({
        full_message: "Please provide valid polling interval",
        nice_message:
          "Minimum allowable polling interval is 5 minutes, maximum allowable polling interval is 7 days",
      });
    }
  };

  return (
    <FormWrap
      onSubmit={handleSubmit}
      buttonContent="Proceed"
      title=""
      error={formError}
      description=""
    >
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
        help="Determines how often (in minutes) Graylog will check for new data in Database . The smallest allowable interval is 1 minute."
        label="Polling Interval"
      />

      <ValidatedInput type="select"
        id="stateFieldType"
        onChange={onChange}
        fieldData={formData.stateFieldType}
        help="There are two choices for this field: Timestamp or increment."
        required
        label="State Field Type">
        {renderOptions(fieldTypes, 'select the checkpointing field', false)}
      </ValidatedInput>

      <ValidatedInput id="stateField"
        type="text"
        onChange={onChange}
        fieldData={formData.stateField}
        help="Enter the column name that is used to track its collection state(Checkpoint). The column name entered in the State Field Type must be included in the Query Statement"
        label="State Field"
        required />

      <FormAdvancedOptions onChange={onChange} />
    </FormWrap>
  );
};

export default StepSubscribe;