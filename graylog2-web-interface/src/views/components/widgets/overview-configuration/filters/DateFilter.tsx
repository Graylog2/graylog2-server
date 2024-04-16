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
import styled, { css } from 'styled-components';

import { Icon, DatePicker, Switch } from 'components/common';

const Column = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
`;

const Row = styled.div`
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: .6rem;
`;

const DateRow = styled(Row)(({ theme }) => css`
  margin-top: 10px;
  margin-bottom: -10px;
  padding: 5px 10px;
  border-radius: 15px;
  font-size: .9rem;
  font-weight: normal;
  background-color: ${theme.colors.global.background};
  color: ${theme.colors.global.textDefault};
`);

const ClearDate = styled(Icon)(({ theme }) => css`
  padding-left: 5px;
  cursor: pointer;
  color: ${theme.colors.variant.default};
`);

type Props = {
  values: Array<string>,
  onChange: (arg: Array<string>) => void,
};

const DateFilter = ({ values = [], onChange }: Props) => {
  const [currentDate, setCurrentDate] = React.useState<string>(null);
  const [dateRange, setDateRange] = React.useState<boolean>(false);

  const toggleDateRange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const auxDateRange = e.target.checked;
    if (!auxDateRange && values.length === 2) onChange([]);
    setDateRange(auxDateRange);
  };

  const filterIncluded = (fDate: string) => !!values.find((value) => value === fDate);

  const onDateChange = (day: Date, _, e: React.BaseSyntheticEvent) => {
    e.stopPropagation();
    const isoDate = [
      day.getFullYear(),
      (day.getMonth() + 1).toString().padStart(2, '0'),
      day.getDate().toString().padStart(2, '0'),
    ].join('-');

    if (filterIncluded(isoDate)) {
      onChange(values.filter((value) => value !== isoDate));
    } else if (values.length === 2 || !dateRange) {
      onChange([isoDate]);
    } else {
      onChange([...values, isoDate]
        .sort((d1, d2) => {
          const d1Int = new Date(d1).getTime();
          const d2Int = new Date(d2).getTime();

          if (d1Int > d2Int) return 1;
          if (d1Int < d2Int) return -1;

          return 0;
        }),
      );
    }

    setCurrentDate(isoDate);
  };

  return (
    <Column>
      <Row>
        <span>Single Date</span>
        <Switch checked={dateRange}
                aria-label={`Select type ${dateRange ? 'single date' : 'range'}`}
                onChange={toggleDateRange} />
        <span>Range</span>
      </Row>
      {values.length > 0 && (
        <DateRow>
          {values.join(' to ')}
          <ClearDate data-testid="clear-date"
                     name="close"
                     size="xs"
                     onClick={() => onChange([])} />
        </DateRow>
      )}
      <DatePicker date={currentDate} onChange={onDateChange} />
    </Column>
  );
};

export default DateFilter;
