// @flow strict
import * as React from 'react';
import { FastField } from 'formik';

import { Input } from 'components/bootstrap';
import { TimezoneSelect } from 'components/common';

const TimezoneFormGroup = () => (
  <FastField name="timezone">
    {({ field: { name, value, onChange } }) => (
      <Input id="timezone-select"
             label="Time Zone"
             help="Choose your local time zone or leave it as it is to use the system's default."
             labelClassName="col-sm-3"
             wrapperClassName="col-sm-9">
        <TimezoneSelect className="timezone-select"
                        value={value}
                        name="timezone"
                        onChange={(newValue) => onChange({ target: { name, value: newValue } })} />
      </Input>
    )}
  </FastField>
);

export default TimezoneFormGroup;
