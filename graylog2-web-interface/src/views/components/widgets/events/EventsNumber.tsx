import React from 'react';
import styled from 'styled-components';

import type { WidgetComponentProps } from 'views/types';
import AutoFontSizer from 'views/components/visualizations/number/AutoFontSizer';
import { ElementDimensions } from 'components/common';
import type EventsWidgetConfig from 'views/logic/widgets/events/EventsWidgetConfig';

import type { EventsListResult } from './types';

const NumberBox = styled(ElementDimensions)`
  height: 100%;
  width: 100%;
  padding-bottom: 10px;
`;

const EventsNumber = ({ data } : WidgetComponentProps<EventsWidgetConfig, EventsListResult>) => (
  <NumberBox resizeDelay={20}>
    {({ height, width }) => (
      <AutoFontSizer height={height} width={width} center>
        <div>
          {data.totalResults}
        </div>
      </AutoFontSizer>
    )}
  </NumberBox>
);

export default EventsNumber;
