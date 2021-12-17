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
import numeral from 'numeral';

import Icon from 'components/common/Icon';
import type { TrendPreference } from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';

type TrendDirection = 'good' | 'bad' | 'neutral';

type Props = {
  current: number,
  previous: number | undefined | null,
  trendPreference: TrendPreference,
};

const background = (theme: DefaultTheme, trend: TrendDirection = 'neutral') => ({
  good: theme.colors.variant.success,
  bad: theme.colors.variant.danger,
  neutral: theme.colors.global.contentBackground,
}[trend]);

const Background = styled.div<{ trend: TrendDirection | undefined }>(({ theme, trend }) => {
  const bgColor = background(theme, trend);

  return css`
    text-align: right;
    ${trend && css`
      background-color: ${bgColor} !important; /* Needed for report generation */
      color: ${theme.utils.contrastingColor(bgColor)} !important /* Needed for report generation */;
      color-adjust: exact !important; /* Needed for report generation */
    `}
  `;
});

const TextContainer = styled.div<{ trend: TrendDirection | undefined, ref }>(({ theme, trend }) => {
  const bgColor = background(theme, trend);

  return css`
      margin: 5px;
      color: ${theme.utils.contrastingColor(bgColor)} !important /* Needed for report generation */;
      font-family: ${theme.fonts.family.body};
      color-adjust: exact !important; /* Needed for report generation */`;
});

const StyledIcon = styled(Icon)<{ trend: TrendDirection | undefined }>(({ theme, trend }) => {
  const bgColor = background(theme, trend);

  return css`
    path {
      fill: ${theme.utils.contrastingColor(bgColor)};
    }`;
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

// eslint-disable-next-line no-nested-ternary
const _trendIcon = (delta: number) => (delta === 0
  ? 'arrow-circle-right'
  : delta > 0
    ? 'arrow-circle-up'
    : 'arrow-circle-down');

const diff = (current: number | undefined, previous: number | undefined): [number, number] => {
  if (typeof current === 'number' && typeof previous === 'number') {
    const difference = current - previous;
    const differencePercent = difference / previous;

    return [difference, differencePercent];
  }

  return [NaN, NaN];
};

const Trend = React.forwardRef<HTMLSpanElement, Props>(({ current, previous, trendPreference }: Props, ref) => {
  const [difference, differencePercent] = diff(current, previous);

  const backgroundTrend = _trendDirection(difference, trendPreference);
  const trendIcon = _trendIcon(difference);

  const absoluteDifference = Number.isFinite(difference) ? numeral(difference).format('+0,0[.]0[000]') : '--';
  const relativeDifference = Number.isFinite(differencePercent) ? numeral(differencePercent).format('+0[.]0[0]%') : '--';

  return (
    <Background trend={backgroundTrend} data-testid="trend-background">
      <TextContainer trend={backgroundTrend} ref={ref}>
        <StyledIcon name={trendIcon} trend={backgroundTrend} data-testid="trend-icon" /> <span data-testid="trend-value" title={`Previous value: ${previous}`}>{absoluteDifference} / {relativeDifference}</span>
      </TextContainer>
    </Background>
  );
});

export default Trend;
