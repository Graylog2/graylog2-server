// @flow strict
import * as React from 'react';
import styled from 'styled-components';
import numeral from 'numeral';
import teinte from 'theme/teinte';
import contrastingColor from 'util/contrastingColor';

import Icon from 'components/common/Icon';
import NumberVisualizationConfig from 'views/logic/aggregationbuilder/visualizations/NumberVisualizationConfig';

type Props = {
  current: number,
  previous: ?number,
  config: NumberVisualizationConfig,
};

const Shared = styled.div`
  text-align: right;
`;
const GoodBackground: React.AbstractComponent<{}> = styled(Shared)`
  background-color: ${teinte.tertiary.tre};
  color: ${contrastingColor(teinte.tertiary.tre)};
`;
const BadBackground: React.AbstractComponent<{}> = styled(Shared)`
  background-color: ${teinte.tertiary.quattro};
  color: ${contrastingColor(teinte.tertiary.quattro)};
`;

const NeutralBackground: React.AbstractComponent<{}> = styled(Shared)``;

const TextContainer = styled.span`
  margin: 5px;
`;

const _background = (delta, lowerIsBetter) => {
  if (delta === 0) {
    return NeutralBackground;
  }
  if (lowerIsBetter) {
    return delta > 0 ? BadBackground : GoodBackground;
  }

  return delta > 0 ? GoodBackground : BadBackground;
};

const _trendIcon = (delta) => {
  if (delta === 0) {
    return <Icon name="arrow-circle-right" />;
  }

  return delta > 0 ? <Icon name="arrow-circle-up" /> : <Icon name="arrow-circle-down" />;
};

const Trend = React.forwardRef<Props, any>(({ current, previous, config }: Props, ref) => {
  const difference = previous ? current - previous : NaN;
  const differencePercent = previous ? difference / previous : NaN;

  const Background = _background(difference, config.lowerIsBetter);
  const trendIcon = _trendIcon(difference);

  return (
    <Background>
      <TextContainer ref={ref}>
        {trendIcon} {numeral(difference).format('+0,0[.]0[000]')} / {numeral(differencePercent).format('+0[.]0[0]%')}
      </TextContainer>
    </Background>
  );
});

export default Trend;
