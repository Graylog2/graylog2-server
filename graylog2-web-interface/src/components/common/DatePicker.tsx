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
import PropTypes from 'prop-types';
import React from 'react';
import type { DayModifiers } from 'react-day-picker';
import DayPicker from 'react-day-picker';
import styled, { css } from 'styled-components';

import 'react-day-picker/lib/style.css';
import { toDateObject, adjustFormat } from 'util/DateTime';

const StyledDayPicker = styled(DayPicker)(({ theme }) => css`
  width: 100%;

  .DayPicker-Day {
    min-width: 34px;
    max-width: 34px;
    min-height: 34px;
    max-height: 34px;
    
    &--selected:not(.DayPicker-Day--disabled, .DayPicker-Day--outside) {
      background-color: ${theme.colors.variant.lighter.primary};
      color: ${theme.colors.variant.darkest.primary};
    }
    
    &--today {
      color: ${theme.colors.variant.info};
    }
    
    &:focus {
      outline-color: ${theme.colors.variant.primary};
    }
  }
  
  &:not(.DayPicker--interactionDisabled) .DayPicker-Day:not(.DayPicker-Day--disabled, .DayPicker-Day--selected, .DayPicker-Day--outside) {
    &:focus {
      outline-color: ${theme.colors.variant.primary};
    }
    
    &:hover {
      background-color: ${theme.colors.variant.lightest.primary};
      color: ${theme.colors.variant.darker.primary};
    }
  }
`);

type Props = {
  date?: string | undefined,
  onChange: (day: Date, modifiers: DayModifiers, event: React.MouseEvent<HTMLDivElement>) => void,
  fromDate: Date,
  showOutsideDays: boolean,
};

const DatePicker = ({ date, fromDate, onChange, showOutsideDays }: Props) => {
  let selectedDate;

  if (date) {
    try {
      selectedDate = toDateObject(date);
    } catch (e) {
      // don't do anything
    }
  }

  const modifiers = {
    selected: (moddedDate) => {
      if (!selectedDate) {
        return false;
      }

      const dateTime = toDateObject(adjustFormat(moddedDate, 'complete'));

      return (adjustFormat(selectedDate, 'date') === adjustFormat(dateTime, 'date'));
    },
    disabled: {
      before: new Date(fromDate),
    },
  };

  return (
    <StyledDayPicker initialMonth={selectedDate ? selectedDate.toDate() : undefined}
                     onDayClick={onChange}
                     modifiers={modifiers}
                     showOutsideDays={showOutsideDays} />
  );
};

DatePicker.propTypes = {
  /** Initial date to select in the date picker. */
  date: PropTypes.string,
  /**
   * Callback that will be called when user picks a date. It will receive the new selected day,
   * `react-day-picker`'s modifiers, and the original event as arguments.
   */
  onChange: PropTypes.func.isRequired,
  /** Earliest date possible to select in the date picker. */
  fromDate: PropTypes.instanceOf(Date),
  /** Earliest date possible to select in the date picker. */
  showOutsideDays: PropTypes.bool,
};

DatePicker.defaultProps = {
  date: undefined,
  fromDate: undefined,
  showOutsideDays: false,
};

export default DatePicker;
