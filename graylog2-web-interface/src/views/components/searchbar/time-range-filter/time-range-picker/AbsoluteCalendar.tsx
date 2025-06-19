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
import * as React from 'react';
import { Field } from 'formik';

import type { AbsoluteTimeRange } from 'views/logic/queries/Query';
import DateTimePicker from 'views/components/searchbar/time-range-filter/time-range-picker/DateTimePicker';

type Props = {
  startDate?: Date;
  range: 'to' | 'from';
  timeRange: AbsoluteTimeRange;
};

const AbsoluteCalendar = ({ startDate = undefined, timeRange, range }: Props) => (
  <Field name={`timeRangeTabs.absolute.${range}`}>
    {({ field: { value, onChange, name }, meta: { error } }) => {
      const _onChange = (newValue: string) => onChange({ target: { name, value: newValue } });
      const dateTime = error ? timeRange[range] : value || timeRange[range];

      return <DateTimePicker error={error} onChange={_onChange} value={dateTime} range={range} startDate={startDate} />;
    }}
  </Field>
);

export default AbsoluteCalendar;
