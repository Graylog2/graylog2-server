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

import Icon from 'components/common/Icon';
import type FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import { getPrettifiedValue, convertValueToUnit } from 'views/components/visualizations/utils/unitConverters';
import formatValueWithUnitLabel from 'views/components/visualizations/utils/formatValueWithUnitLabel';
import getUnitTextLabel from 'views/components/visualizations/utils/getUnitTextLabel';
import { formatTrend } from 'util/NumberFormatting';

import { diff } from './trendDirection';

const TextContainer = styled.div(
  ({ theme }) => css`
    margin: 5px;
    text-align: right;
    font-family: ${theme.fonts.family.body};
  `,
);

const _trendIcon = (delta: number) =>
  // eslint-disable-next-line no-nested-ternary
  delta === 0 ? 'arrow_circle_right' : delta > 0 ? 'arrow_circle_up' : 'arrow_circle_down';

const getTrendConvertedValues = (
  current: number,
  previous: number,
  fieldUNit: FieldUnit,
): {
  previousConverted: number | string;
  differenceConverted: number;
  differencePercent: number;
  unitAbbrevString: string;
} => {
  const [difference, differencePercent] = diff(current, previous);

  if (!fieldUNit?.isDefined) {
    return {
      previousConverted: previous,
      differenceConverted: difference,
      differencePercent,
      unitAbbrevString: '',
    };
  }

  const originalParams = { unitType: fieldUNit?.unitType, abbrev: fieldUNit?.abbrev };
  const { unit: currentPrettyUnit } = getPrettifiedValue(current, originalParams);
  const currentPrettyParams = { unitType: currentPrettyUnit?.unitType, abbrev: currentPrettyUnit?.abbrev };
  const { value: prettyDiff } = convertValueToUnit(difference, originalParams, currentPrettyParams);
  const { value: previousPretty } = convertValueToUnit(previous, originalParams, currentPrettyParams);

  return {
    previousConverted: `${formatValueWithUnitLabel(previousPretty, currentPrettyUnit.abbrev)} (${previous})`,
    unitAbbrevString: ` ${getUnitTextLabel(currentPrettyUnit.abbrev)}`,
    differenceConverted: prettyDiff,
    differencePercent,
  };
};

type Props = {
  current: number;
  previous: number | undefined | null;
  unit?: FieldUnit;
};

const Trend = ({ current, previous, unit = undefined }: Props, ref: React.ForwardedRef<HTMLDivElement>) => {
  const { differenceConverted, differencePercent, unitAbbrevString, previousConverted } = getTrendConvertedValues(
    current,
    previous,
    unit,
  );

  const trendIcon = _trendIcon(differenceConverted);

  const absoluteDifference = Number.isFinite(differenceConverted)
    ? `${formatTrend(differenceConverted)}${unitAbbrevString}`
    : '--';
  const relativeDifference = Number.isFinite(differencePercent)
    ? formatTrend(differencePercent, { percentage: true })
    : '--';

  return (
    <TextContainer ref={ref}>
      <Icon name={trendIcon} data-testid="trend-icon" />{' '}
      <span data-testid="trend-value" title={`Previous value: ${previousConverted}`}>
        {absoluteDifference} / {relativeDifference}
      </span>
    </TextContainer>
  );
};

Trend.displayName = 'Trend';

export default React.forwardRef(Trend);
