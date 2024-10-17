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
import React, { useMemo } from 'react';
import type { DayModifiers } from 'react-day-picker';
import DayPicker from 'react-day-picker';
import styled, { css } from 'styled-components';

import 'react-day-picker/lib/style.css';

import { isValidDate, toDateObject, adjustFormat } from 'util/DateTime';

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

const useSelectedDate = (date: string | undefined) => useMemo(() => {
  const isDateValid = !date || isValidDate(toDateObject(date, ['date']));

  if (isDateValid) {
    return date;
  }

  return undefined;
}, [date]);

type Props = {
  date?: string | undefined,
  onChange: (day: Date, modifiers: DayModifiers, event: React.MouseEvent<HTMLDivElement>) => void,
  fromDate?: Date,
  showOutsideDays?: boolean,
};

const DatePicker = ({ date, fromDate, onChange, showOutsideDays = false }: Props) => {
  const selectedDate = useSelectedDate(date);

  const modifiers = useMemo(() => ({
    selected: (moddedDate: Date) => {
      if (!selectedDate) {
        return false;
      }

      return selectedDate === adjustFormat(moddedDate, 'date');
    },
    disabled: {
      before: new Date(fromDate),
    },
  }), [fromDate, selectedDate]);

  return (
    <StyledDayPicker initialMonth={selectedDate ? toDateObject(selectedDate).toDate() : undefined}
                     onDayClick={onChange}
                     modifiers={modifiers}
                     showOutsideDays={showOutsideDays} />
  );
};

export default DatePicker;
