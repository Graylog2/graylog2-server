// @flow strict
import * as React from 'react';
import styled, { type StyledComponent, css } from 'styled-components';
import numeral from 'numeral';

import { util, type ThemeInterface } from 'theme';

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
  const { variant } = theme.color;
  const bgColor = trend && trend === TREND_GOOD ? variant.success : variant.primary;

  return css`
    text-align: right;
    ${trend && css`
      background-color: ${bgColor} !important; /* Needed for report generation */
      color: ${util.contrastingColor(bgColor)} !important /* Needed for report generation */;

      /* stylelint-disable-next-line property-no-vendor-prefix */
      -webkit-print-color-adjust: exact !important; /* Needed for report generation */
    `}
  `;
});

const TextContainer: StyledComponent<{trend: ?string}, ThemeInterface, HTMLDivElement> = styled.div(({ theme, trend }) => {
  const { variant } = theme.color;
  const bgColor = trend && trend === TREND_GOOD ? variant.success : variant.primary;

  return css`
      margin: 5px;
      color: ${util.contrastingColor(bgColor)} !important /* Needed for report generation */;

      /* stylelint-disable-next-line property-no-vendor-prefix */
      -webkit-print-color-adjust: exact !important; /* Needed for report generation */`;
});

const StyledIcon: StyledComponent<{trend: ?string}, ThemeInterface, typeof Icon> = styled(Icon)(({ theme, trend }) => {
  const { variant } = theme.color;
  const bgColor = trend && trend === TREND_GOOD ? variant.success : variant.primary;

  return css`
    path {
      fill: ${util.contrastingColor(bgColor)};
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

const _trendIcon = (delta, trend) => {
  if (delta === 0) {
    return <StyledIcon trend={trend} name="arrow-circle-right" />;
  }

  return <StyledIcon trend={trend} name={delta > 0 ? 'arrow-circle-up' : 'arrow-circle-down'} />;
};

const Trend = React.forwardRef<Props, any>(({ current, previous, trendPreference }: Props, ref) => {
  const difference = previous ? current - previous : NaN;
  const differencePercent = previous ? difference / previous : NaN;

  const backgroundTrend = _background(difference, trendPreference);
  const trendIcon = _trendIcon(difference, backgroundTrend);

  return (
    <Background trend={backgroundTrend} data-test-id="trend-background">
      <TextContainer trend={backgroundTrend} ref={ref}>
        {trendIcon} {numeral(difference).format('+0,0[.]0[000]')} / {numeral(differencePercent).format('+0[.]0[0]%')}
      </TextContainer>
    </Background>
  );
});

export default Trend;
