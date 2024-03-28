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

import { Icon, DatePicker } from 'components/common';

const Toggle = styled.label(({ theme }) => css`
  display: flex;
  align-items: center;
  margin: 0;

  input {
    border: 0;
    clip: rect(0 0 0 0);
    clip-path: inset(50%);
    height: 1px;
    margin: -1px;
    overflow: hidden;
    padding: 0;
    position: absolute;
    width: 1px;
    white-space: nowrap;

    &:checked + .slider {
      background-color: ${theme.colors.variant.success};

      &::before {
        transform: translate(16px, -50%);
      }
    }

    &:disabled + .slider {
      opacity: 0.5;
      cursor: not-allowed;

      &::before {
        background-color: ${theme.colors.variant.light.default};
      }
    }
  }

  .slider {
    box-sizing: border-box;
    margin: 0 9px;
    width: 36px;
    height: 22px;
    border-radius: 30px;
    background-color: ${theme.colors.gray[80]};
    box-shadow: inset 0 1px 3px 0 rgb(0 0 0 / 20%);
    display: inline-block;
    position: relative;
    cursor: pointer;

    &::before {
      transition: transform 75ms ease-in-out;
      content: '';
      display: block;
      width: 18px;
      height: 18px;
      background-color: #fcfcfc;
      box-shadow: 0 2px 3px 0 rgb(0 0 0 / 25%), 0 2px 8px 0 rgb(32 37 50 / 16%);
      position: absolute;
      border-radius: 100%;
      top: 11px;
      transform: translate(2px, -50%);
    }
  }
`);

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

  const toggleDateRange = (e: React.BaseSyntheticEvent) => {
    e.stopPropagation();
    e.preventDefault();

    const auxDateRange = !dateRange;
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
        <Toggle data-testid="range-toggle" onClick={toggleDateRange}>
          <input type="checkbox" onChange={toggleDateRange} checked={dateRange} />
          <span className="slider" />
        </Toggle>
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
