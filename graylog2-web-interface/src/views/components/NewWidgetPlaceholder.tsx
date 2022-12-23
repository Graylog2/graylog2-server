import * as React from 'react';
import styled, { css } from 'styled-components';

import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { CurrentViewStateActions } from 'views/stores/CurrentViewStateStore';
import { Icon } from 'components/common';
import Series from 'views/logic/aggregationbuilder/Series';
import SeriesConfig from 'views/logic/aggregationbuilder/SeriesConfig';
import { WidgetActions } from 'views/stores/WidgetStore';
import AggregationWidget from 'views/logic/aggregationbuilder/AggregationWidget';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import NumberVisualization from 'views/components/visualizations/number/NumberVisualization';
import generateId from 'logic/generateId';

const PlaceholderBox = styled.div(({ theme }) => css`
  opacity: 0;
  transition: visibility 0s, opacity 0.2s linear;
  
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;  
  
  background-color: ${theme.colors.global.contentBackground};
  color: ${theme.colors.gray[30]};
  margin-bottom: ${theme.spacings.xs};
  
  :hover {
    opacity: 1;
  }
`);

const HugeIcon = styled(Icon)(({ theme }) => css`
  font-size: ${theme.fonts.size.huge};
  margin-bottom: 10px;
`);

type Props = {
  style?: React.CSSProperties,
  position: WidgetPosition,
}

const NewWidgetPlaceholder = React.forwardRef<HTMLDivElement, Props>(({ style, position }, ref) => {
  const containerStyle = {
    ...style,
    transition: 'none',
  };

  const onClick = async () => {
    const newId = generateId();
    await CurrentViewStateActions.updateWidgetPosition(newId, position);
    const series = Series.forFunction('count()')
      .toBuilder()
      .config(new SeriesConfig('Message Count'))
      .build();

    return WidgetActions.create(AggregationWidget.builder()
      .id(newId)
      .config(AggregationWidgetConfig.builder()
        .series([series])
        .visualization(NumberVisualization.type)
        .build())
      .build());
  };

  return (
    <div style={containerStyle} ref={ref}>
      <PlaceholderBox onClick={onClick}>
        <HugeIcon name="circle-plus" />
        Create a new widget here
      </PlaceholderBox>
    </div>
  );
});

NewWidgetPlaceholder.defaultProps = {
  style: {},
};

export default NewWidgetPlaceholder;
