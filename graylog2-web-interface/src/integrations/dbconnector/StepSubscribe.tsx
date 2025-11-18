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
import React, { useContext, useState, useEffect } from "react";

import { Input } from 'components/bootstrap';

import FormAdvancedOptions from './FormAdvancedOptions';
import { ApiRoutes } from './Routes';

import FormWrap from '../common/FormWrap';
import { FormDataContext } from '../common/context/FormData';
import type {
  ErrorMessageType,
  FormDataContextType,
  HandleSubmitType,
} from '../common/utils/types';
import ValidatedInput from "../common/ValidatedInput";
import { renderOptions } from "../common/Options";


interface Props {
  onSubmit: HandleSubmitType;
  onChange: (...args: any[]) => void;
}

const fieldTypes = [{ label: 'Timestamp', value: 'Timestamp' },
{ label: 'increment', value: 'increment' }];
const StepSubscribe: React.FC<Props> = ({ onSubmit, onChange }) => {
  const { formData } = useContext<FormDataContextType>(FormDataContext);
  const [formError, setFormError] = useState<ErrorMessageType>(null);
  const { pollingInterval } = formData;
  const selectedFieldType = formData.stateFieldType?.value;

  const [timezones, setTimezones] = useState<{ label: string, value: string }[]>([]);
  const [loadingTz, setLoadingTz] = useState(false);
  const [tzError, setTzError] = useState<string | null>(null);

  useEffect(() => {
    if (selectedFieldType === 'Timestamp') {
      setLoadingTz(true);
      fetch(`/api${ApiRoutes.INTEGRATIONS.DBConnector.TIMEZONES}`)
        .then((res) => res.json())
        .then((data) => {
          const zones = data.regions || [];
          const options = zones.map((zone: string) => ({ label: zone, value: zone }));
          setTimezones(options);
          setLoadingTz(false);
        })
        .catch(() => {
          setTzError("Failed to load timezones");
          setLoadingTz(false);
        });
    }
  }, [selectedFieldType]);


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
      className="col-md-7"
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

      {selectedFieldType === 'Timestamp' && (
        <ValidatedInput
          id="timezone"
          type="select"
          onChange={onChange}
          fieldData={formData.timezone}
          help="Select the timezone used by the source database for storing timestamp values."
          label="Timezone"
          required
        >
          {loadingTz && <option>Loading timezones...</option>}
          {tzError && <option disabled>{tzError}</option>}
          {!loadingTz && !tzError && renderOptions(timezones, 'Select a timezone', false)}
        </ValidatedInput>
      )}

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