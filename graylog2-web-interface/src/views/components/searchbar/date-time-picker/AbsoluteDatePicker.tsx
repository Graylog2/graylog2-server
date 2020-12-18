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
import moment from 'moment';

import { DatePicker } from 'components/common';
import DateTime from 'logic/datetimes/DateTime';

const AbsoluteDatePicker = ({ name, disabled, dateTime, onChange, startDate }) => {
  const initialDateTime = moment(dateTime).toObject();

  const _onDatePicked = (date) => {
    if (!!startDate && DateUtils.isDayBefore(date, startDate)) {
      return false;
    }

    const newDate = moment(date).toObject();

    return onChange(moment({
      ...initialDateTime,
      years: newDate.years,
      months: newDate.months,
      date: newDate.date,
    }).format(DateTime.Formats.TIMESTAMP));
  };

  return (
    <DatePicker id={`date-input-datepicker-${name}`}
                disabled={disabled}
                title={`Search ${name} date`}
                date={dateTime}
                onChange={_onDatePicked}
                fromDate={startDate} />
  );
};

AbsoluteDatePicker.propTypes = {
  name: PropTypes.string.isRequired,
  dateTime: PropTypes.string.isRequired,
  disabled: PropTypes.bool,
  onChange: PropTypes.func,
  startDate: PropTypes.instanceOf(Date).isRequired,
};

AbsoluteDatePicker.defaultProps = {
  disabled: false,
  onChange: () => {},
};

export default AbsoluteDatePicker;
