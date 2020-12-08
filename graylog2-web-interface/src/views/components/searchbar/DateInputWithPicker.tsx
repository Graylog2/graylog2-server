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
import moment from 'moment';
import { DateUtils } from 'react-day-picker';
import type { StyledComponent } from 'styled-components';
import styled, { css } from 'styled-components';

import { DatePicker, Icon } from 'components/common';
import { Button } from 'components/graylog';
import DateTime from 'logic/datetimes/DateTime';
import { Input } from 'components/bootstrap';
import type { ThemeInterface } from 'theme';

type Props = {
  disabled: boolean | undefined | null,
  error: string | undefined | null,
  value: string,
  onChange: (date: string) => void,
  name: string,
  title: string | undefined | null,
  initialDateTimeObject: {
    years: string | number,
    months: string | number,
    date: string | number,
    hours: string | number,
    minutes: string | number,
    seconds: string | number,
    milliseconds: string | number,
  },
  fromDate?: Date,
};

const ErrorMessage: StyledComponent<{}, ThemeInterface, HTMLSpanElement> = styled.span(({ theme }) => css`
  color: ${theme.colors.variant.dark.danger};
  font-size: ${theme.fonts.size.tiny};
  font-style: italic;
  padding: 3px 3px 9px;
`);

const DateInputWithPicker = ({ disabled = false, error, fromDate, value, onChange, name, title, initialDateTimeObject }: Props) => {
  const _onDatePicked = (date) => {
    if (!!fromDate && DateUtils.isDayBefore(date, fromDate)) {
      return false;
    }

    const newDate = moment(date).toObject();

    // @ts-ignore
    return onChange(moment({
      ...initialDateTimeObject,
      years: newDate.years,
      months: newDate.months,
      date: newDate.date,
    }).format(DateTime.Formats.TIMESTAMP));
  };

  const _onChangeInput = (event) => onChange(event.target.value);
  const _onSetTimeToNow = () => onChange(moment().format(DateTime.Formats.TIMESTAMP));

  return (
    <div>
      <Input type="text"
             id={`date-input-${name}`}
             name={name}
             autoComplete="off"
             disabled={disabled}
             onChange={_onChangeInput}
             placeholder={DateTime.Formats.DATETIME}
             value={value}
             buttonAfter={(
               <Button disabled={disabled}
                       onClick={_onSetTimeToNow}
                       title="Insert current date">
                 <Icon name="magic" />
               </Button>
             )}
             bsStyle={error ? 'error' : null} />

      <DatePicker id={`date-input-datepicker-${name}`}
                  disabled={disabled}
                  title={title}
                  date={value}
                  onChange={_onDatePicked}
                  fromDate={fromDate} />

      {error && <ErrorMessage>{error}</ErrorMessage>}
    </div>
  );
};

DateInputWithPicker.propTypes = {
  disabled: PropTypes.bool,
  error: PropTypes.string,
  fromDate: PropTypes.instanceOf(Date),
  value: PropTypes.oneOfType([PropTypes.object, PropTypes.string]),
  onChange: PropTypes.func.isRequired,
  name: PropTypes.string.isRequired,
  title: PropTypes.string,
  initialDateTimeObject: PropTypes.shape({
    years: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    months: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    date: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    hours: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    minutes: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    seconds: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
    milliseconds: PropTypes.oneOfType([PropTypes.string, PropTypes.number]),
  }).isRequired,
};

DateInputWithPicker.defaultProps = {
  disabled: false,
  error: undefined,
  fromDate: undefined,
  value: undefined,
  title: '',
};

export default DateInputWithPicker;
