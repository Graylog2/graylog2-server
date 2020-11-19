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
