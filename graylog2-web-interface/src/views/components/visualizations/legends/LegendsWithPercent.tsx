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
import React from 'react';
import styled, { css } from 'styled-components';

import NumberUtils from 'util/NumberUtils';

const LegendList = styled.ul(
  ({ theme }) => css`
    display: grid;
    grid-template-columns: 1fr;
    gap: ${theme.spacings.xs};
    margin: 0;
    padding: 0;
    overflow-y: auto;
    list-style: none;
    max-height: 300px;
  `,
);

const LegendItem = styled.li`
  display: grid;
  grid-template-columns: auto minmax(0, 1fr) auto;
  gap: ${({ theme }) => theme.spacings.xs};
  align-items: center;
  min-width: 0;
`;

const LegendSwatch = styled.span<{ $color: string }>`
  width: 10px;
  height: 10px;
  background: ${({ $color }) => $color};
`;

const LegendLabel = styled.span`
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
`;

const LegendValue = styled.span`
  color: ${({ theme }) => theme.colors.text.secondary};
  font-size: ${({ theme }) => theme.fonts.size.small};
  white-space: nowrap;
`;

type Props = {
  legends: Array<{
    color: string;
    label: string;
    value: string | number;
    numberValue: number;
    percentageValue: number;
  }>;
};

const LegendsWithPercent = ({ legends }: Props) => (
  <LegendList>
    {legends.map(({ value, label, numberValue, percentageValue, color }) => (
      <LegendItem key={value}>
        <LegendSwatch $color={color} />
        <LegendLabel title={label}>{label}</LegendLabel>
        <LegendValue>
          {NumberUtils.formatNumber(numberValue)} ({NumberUtils.formatPercentage(percentageValue)})
        </LegendValue>
      </LegendItem>
    ))}
  </LegendList>
);

export default LegendsWithPercent;
