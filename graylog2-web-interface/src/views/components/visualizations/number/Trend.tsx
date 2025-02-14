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
import type { DefaultTheme } from 'styled-components';
import styled, { css } from 'styled-components';

import Icon from 'components/common/Icon';
import type { TrendPreference } from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';
import type FieldUnit from 'views/logic/aggregationbuilder/FieldUnit';
import { getPrettifiedValue, convertValueToUnit } from 'views/components/visualizations/utils/unitConverters';
import formatValueWithUnitLabel from 'views/components/visualizations/utils/formatValueWithUnitLabel';
import getUnitTextLabel from 'views/components/visualizations/utils/getUnitTextLabel';
import { formatTrend } from 'util/NumberFormatting';

type TrendDirection = 'good' | 'bad' | 'neutral';

type Props = {
  current: number;
  previous: number | undefined | null;
  trendPreference: TrendPreference;
  // eslint-disable-next-line react/require-default-props
  unit?: FieldUnit;
};

const background = (theme: DefaultTheme, trend: TrendDirection = 'neutral') =>
  ({
    good: theme.colors.variant.success,
    bad: theme.colors.variant.danger,
    neutral: theme.colors.global.contentBackground,
  })[trend];

const Background = styled.div<{ trend: TrendDirection | undefined }>(({ theme, trend }) => {
  const bgColor = background(theme, trend);

  return css`
    text-align: right;
    ${trend &&
    css`
      background-color: ${bgColor} !important; /* Needed for report generation */
      color: ${theme.utils.contrastingColor(bgColor)} !important /* Needed for report generation */;
      color-adjust: exact !important; /* Needed for report generation */
    `}
  `;
});

const TextContainer = styled.div<{ trend: TrendDirection | undefined; ref }>(({ theme, trend }) => {
  const bgColor = background(theme, trend);

  return css`
    margin: 5px;
    color: ${theme.utils.contrastingColor(bgColor)} !important /* Needed for report generation */;
    font-family: ${theme.fonts.family.body};
    color-adjust: exact !important; /* Needed for report generation */
  `;
});

const StyledIcon = styled(Icon)<{ trend: TrendDirection | undefined }>(({ theme, trend }) => {
  const bgColor = background(theme, trend);

  return css`
    path {
      fill: ${theme.utils.contrastingColor(bgColor)};
    }
  `;
});

const _trendDirection = (delta: number, trendPreference: TrendPreference): TrendDirection => {
  switch (trendPreference) {
    case 'LOWER':
      return delta > 0 ? 'bad' : 'good';
    case 'HIGHER':
      return delta > 0 ? 'good' : 'bad';
    case 'NEUTRAL':
    default:
      return 'neutral';
  }
};

const _trendIcon = (delta: number) =>
  // eslint-disable-next-line no-nested-ternary
  delta === 0 ? 'arrow_circle_right' : delta > 0 ? 'arrow_circle_up' : 'arrow_circle_down';

const diff = (current: number | undefined, previous: number | undefined): [number, number] => {
  if (typeof current === 'number' && typeof previous === 'number') {
    const difference = current - previous;
    const differencePercent = difference / previous;

    return [difference, differencePercent];
  }

  return [NaN, NaN];
};

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

const Trend = React.forwardRef<HTMLSpanElement, Props>(
  ({ current, previous, trendPreference, unit = undefined }: Props, ref) => {
    const { differenceConverted, differencePercent, unitAbbrevString, previousConverted } = getTrendConvertedValues(
      current,
      previous,
      unit,
    );

    const backgroundTrend = _trendDirection(differenceConverted, trendPreference);
    const trendIcon = _trendIcon(differenceConverted);

    const absoluteDifference = Number.isFinite(differenceConverted)
      ? `${formatTrend(differenceConverted)}${unitAbbrevString}`
      : '--';
    const relativeDifference = Number.isFinite(differencePercent)
      ? formatTrend(differencePercent, { percentage: true })
      : '--';

    return (
      <Background trend={backgroundTrend} data-testid="trend-background">
        <TextContainer trend={backgroundTrend} ref={ref}>
          <StyledIcon name={trendIcon} trend={backgroundTrend} data-testid="trend-icon" />{' '}
          <span data-testid="trend-value" title={`Previous value: ${previousConverted}`}>
            {absoluteDifference} / {relativeDifference}
          </span>
        </TextContainer>
      </Background>
    );
  },
);
Trend.displayName = 'Trend';

export default Trend;
