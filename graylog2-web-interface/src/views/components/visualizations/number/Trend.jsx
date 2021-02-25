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
// @flow strict
import * as React from 'react';
import styled, { type StyledComponent, css } from 'styled-components';
import numeral from 'numeral';

import type { ThemeInterface } from 'theme';
import Icon from 'components/common/Icon';
import type { TrendPreference } from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';

export const TREND_BAD = 'primary';
export const TREND_GOOD = 'success';
export const TREND_NEUTRAL = undefined;

type Props = {
  current: number,
  previous: ?number,
  trendPreference: TrendPreference,
};

const Background: StyledComponent<{trend: ?string}, ThemeInterface, HTMLDivElement> = styled.div(({ theme, trend }) => {
  const { variant } = theme.colors;
  const bgColor = trend && trend === TREND_GOOD ? variant.success : variant.primary;

  return css`
    text-align: right;
    ${trend && css`
      background-color: ${bgColor} !important; /* Needed for report generation */
      color: ${theme.utils.contrastingColor(bgColor)} !important /* Needed for report generation */;

      /* stylelint-disable-next-line property-no-vendor-prefix */
      -webkit-print-color-adjust: exact !important; /* Needed for report generation */
    `}
  `;
});

const TextContainer = styled.div<{trend: ?string}, ThemeInterface, HTMLDivElement>(({ theme, trend }) => {
  const { variant } = theme.colors;
  const bgColor = trend && trend === TREND_GOOD ? variant.success : variant.primary;

  return css`
      margin: 5px;
      color: ${theme.utils.contrastingColor(bgColor)} !important /* Needed for report generation */;
      font-family: ${theme.fonts.family.body};

      /* stylelint-disable-next-line property-no-vendor-prefix */
      -webkit-print-color-adjust: exact !important; /* Needed for report generation */`;
});

const StyledIcon = styled(Icon)<{ trend: ?string}, ThemeInterface, HTMLDivElement>(({ theme, trend }) => {
  const { variant } = theme.colors;
  const bgColor = trend && trend === TREND_GOOD ? variant.success : variant.primary;

  return css`
    path {
      fill: ${theme.utils.contrastingColor(bgColor)};
    }`;
});

const _background = (delta, trendPreference: TrendPreference) => {
  switch (trendPreference) {
    case 'LOWER':
      return delta > 0 ? TREND_BAD : TREND_GOOD;
    case 'HIGHER':
      return delta > 0 ? TREND_GOOD : TREND_BAD;
    case 'NEUTRAL':
    default:
      return TREND_NEUTRAL;
  }
};

const _trendIcon = (delta) => {
  if (delta === 0) {
    return <StyledIcon name="arrow-circle-right" />;
  }

  return <StyledIcon name={delta > 0 ? 'arrow-circle-up' : 'arrow-circle-down'} />;
};

const Trend = React.forwardRef<Props, any>(({ current, previous, trendPreference }: Props, ref) => {
  const difference = previous ? current - previous : NaN;
  const differencePercent = previous ? difference / previous : NaN;

  const backgroundTrend = _background(difference, trendPreference);
  const trendIcon = _trendIcon(difference);

  return (
    <Background trend={backgroundTrend} data-test-id="trend-background">
      <TextContainer trend={backgroundTrend} ref={ref}>
        {trendIcon} {numeral(difference).format('+0,0[.]0[000]')} / {numeral(differencePercent).format('+0[.]0[0]%')}
      </TextContainer>
    </Background>
  );
});

export default Trend;
