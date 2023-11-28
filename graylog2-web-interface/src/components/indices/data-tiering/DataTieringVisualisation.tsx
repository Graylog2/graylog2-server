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
import styled, { css, useTheme } from 'styled-components';

type Props = {
  minDays?: number,
  maxDays?: number,
  minDaysInHot?: number
}

type BarProps = {
  value: number,
  color: string
}

type LabelProps = {
  value: number,
}

const BarWrapper = styled.div`
overflow: hidden;
`;

const LabelBar = styled.div`
  position: relative;
  height: 50px;
`;

const LifeCycleBar = styled.div(({ theme }) => css`
  height: 40px;
  margin-bottom: 20px;
  background: ${theme.colors.variant.lighter.info};
  position: relative;
`);

const Bar = styled.div<BarProps>(({ theme, value, color }) => css`
    height: 100%;
    font-size: ${theme.fonts.size.small};
    text-align: center;
    flex-shrink: 0;
    background-color: ${color};
    transition: width 500ms ease-in-out;
    position: absolute;
    left: 0;
    width: ${value}%;
    max-width: 100%;
    

    &::after {
      border-right: 1px solid ${theme.colors.variant.darkest.info};
      content: '';
      position: absolute;
      right: 0;
      bottom: 0;
      height: 60px;
      z-index: 1;
    }
`);

const Label = styled.div<LabelProps>(({ value }) => css`
  position: absolute;
  top: 0;
  ${value > 3
    ? `right: ${100 - value}%;`
    : 'left: 0;'}
`);

const DataTieringVisualisation = ({ minDays, maxDays, minDaysInHot }: Props) => {
  const theme = useTheme();

  const percentageFor = (days: number) => {
    if (days <= 0) return 0;

    return ((days / maxDays) * 100);
  };

  return (
    <BarWrapper>
      <LabelBar>
        {percentageFor(minDays) > 0 && (<Label value={percentageFor(minDays)}>{minDays} days</Label>)}
        {percentageFor(minDaysInHot) > 0 && (<Label value={percentageFor(minDaysInHot)}>{minDaysInHot} days</Label>)}
        <Label value={100}>{maxDays} days</Label>
      </LabelBar>
      <LifeCycleBar>
        <Bar value={percentageFor(minDays)} color={theme.colors.variant.info} />
        <Bar value={percentageFor(minDaysInHot)} color={theme.colors.variant.darkest.info} />
      </LifeCycleBar>
    </BarWrapper>
  );
};

export default DataTieringVisualisation;

DataTieringVisualisation.defaultProps = {
  minDays: 0,
  maxDays: 0,
  minDaysInHot: 0,
};
