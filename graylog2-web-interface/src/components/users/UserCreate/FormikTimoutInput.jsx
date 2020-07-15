// @flow strict
import * as React from 'react';
import { Field } from 'formik';

import TimeoutInput from 'components/users/TimeoutInput';

const FomikTimeoutInput = () => (
  <Field name="session_timeout_ms">
    {({ field: { name, value, onChange } }) => (
      <TimeoutInput value={value}
                    labelSize={3}
                    controlSize={9}
                    name={name}
                    onChange={(newValue) => onChange({ target: { name, value: newValue } })} />
    )}
  </Field>
);

export default FomikTimeoutInput;
