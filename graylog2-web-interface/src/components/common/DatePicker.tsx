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
import type { Modifiers } from 'react-day-picker';
import { DayPicker } from 'react-day-picker';
import styled, { css } from 'styled-components';
import 'react-day-picker/style.css';

import { isValidDate, toDateObject, adjustFormat } from 'util/DateTime';

const StyledDayPicker = styled(DayPicker)(
  ({ theme }) => css`
    width: 100%;

    --rdp-accent-color: ${theme.colors.variant.primary};
    --rdp-disabled-opacity: 0.4;
    --rdp-day-width: 34px;
    --rdp-day-height: 34px;
    --rdp-day_button-width: 34px;
    --rdp-day_button-height: 34px;

    .rdp-chevron {
      fill: ${theme.colors.gray[60]};
    }

    .rdp-months {
      margin: 0 auto;
    }
  `,
);

const useSelectedDate = (date: string | undefined) =>
  useMemo(() => {
    const isDateValid = !date || isValidDate(toDateObject(date, ['date']));

    if (isDateValid) {
      return date;
    }

    return undefined;
  }, [date]);

type Props = {
  date?: string | undefined;
  onChange: (day: Date, modifiers: Modifiers, event: React.MouseEvent<HTMLDivElement>) => void;
  fromDate?: Date;
  showOutsideDays?: boolean;
};

const DatePicker = ({ date = undefined, fromDate = undefined, onChange, showOutsideDays = false }: Props) => {
  const selectedDate = useSelectedDate(date);

  const modifiers = useMemo(
    () => ({
      selected: (moddedDate: Date) => {
        if (!selectedDate) {
          return false;
        }

        return selectedDate === adjustFormat(moddedDate, 'date');
      },
      disabled: {
        before: new Date(fromDate),
      },
    }),
    [fromDate, selectedDate],
  );

  return (
    <StyledDayPicker
      defaultMonth={selectedDate ? toDateObject(selectedDate).toDate() : undefined}
      onDayClick={onChange}
      modifiers={modifiers}
      timeZone="UTC"
      showOutsideDays={showOutsideDays}
    />
  );
};

export default DatePicker;
