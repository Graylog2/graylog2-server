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
import PropTypes from 'prop-types';
import { DateUtils } from 'react-day-picker';

import { DatePicker } from 'components/common';
import { toUTCFromTz, toDateObject } from 'util/DateTime';
import useUserDateTime from 'hooks/useUserDateTime';

type Props = {
  dateTime: string,
  onChange?: (string) => void,
  startDate?: Date,
}

const AbsoluteDatePicker = ({ dateTime, onChange, startDate }: Props) => {
  const { userTimezone, formatTime } = useUserDateTime();
  const initialDateTime = toUTCFromTz(dateTime, userTimezone);
  const initialDate = formatTime(initialDateTime, 'date');

  const _onDatePicked = (selectedDate: Date) => {
    if (!!startDate && DateUtils.isDayBefore(selectedDate, startDate)) {
      return false;
    }

    const selectedDateObject = toDateObject(selectedDate);
    const newDate = initialDateTime.set({
      year: selectedDateObject.year(),
      month: selectedDateObject.month(),
      date: selectedDateObject.date(),
    });

    return onChange(formatTime(newDate, 'default'));
  };

  return (
    <DatePicker date={initialDate}
                onChange={_onDatePicked}
                fromDate={startDate} />
  );
};

AbsoluteDatePicker.propTypes = {
  dateTime: PropTypes.string.isRequired,
  onChange: PropTypes.func,
  startDate: PropTypes.instanceOf(Date),
};

AbsoluteDatePicker.defaultProps = {
  onChange: () => {},
  startDate: undefined,
};

export default AbsoluteDatePicker;
