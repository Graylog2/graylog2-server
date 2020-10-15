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
import DayPicker from 'react-day-picker';
import styled, { css } from 'styled-components';

import DateTime from 'logic/datetimes/DateTime';

import 'react-day-picker/lib/style.css';

const StyledDayPicker = styled(DayPicker)(({ theme }) => css`
  width: 100%;

  .DayPicker-Day {
    min-width: 34px;
    max-width: 34px;
    min-height: 34px;
    max-height: 34px;
    
    &--selected:not(.DayPicker-Day--disabled):not(.DayPicker-Day--outside) {
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
  
  &:not(.DayPicker--interactionDisabled) .DayPicker-Day:not(.DayPicker-Day--disabled):not(.DayPicker-Day--selected):not(.DayPicker-Day--outside) {
    &:focus {
      outline-color: ${theme.colors.variant.primary};
    }
    
    &:hover {
      background-color: ${theme.colors.variant.lightest.primary};
      color: ${theme.colors.variant.darker.primary};
    }
  }
`);

/**
 * Component that renders a given children and wraps a date picker around it. The date picker will show when
 * the children is clicked, and hidden when clicking somewhere else.
 */
const DatePicker = ({ date, onChange }) => {
  let selectedDate;

  if (date) {
    try {
      selectedDate = DateTime.parseFromString(date);
    } catch (e) {
      // don't do anything
    }
  }

  const modifiers = {
    selected: (moddedDate) => {
      if (!selectedDate) {
        return false;
      }

      const dateTime = DateTime.ignoreTZ(moddedDate);

      return (selectedDate.toString(DateTime.Formats.DATE) === dateTime.toString(DateTime.Formats.DATE));
    },
  };

  return (
    <StyledDayPicker initialMonth={selectedDate ? selectedDate.toDate() : undefined}
                     onDayClick={onChange}
                     modifiers={modifiers}
                     showOutsideDays />
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
};

DatePicker.defaultProps = {
  date: undefined,
};

export default DatePicker;
