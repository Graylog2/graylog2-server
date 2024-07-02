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
import React, { useContext, useEffect } from 'react';
import styled from 'styled-components';

import type { WidgetComponentProps } from 'views/types';
import AutoFontSizer from 'views/components/visualizations/number/AutoFontSizer';
import { ElementDimensions } from 'components/common';
import type EventsWidgetConfig from 'views/logic/widgets/events/EventsWidgetConfig';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';

import type { EventsListResult } from './types';

const NumberBox = styled(ElementDimensions)`
  height: 100%;
  width: 100%;
  padding-bottom: 10px;
`;

const EventsNumber = ({ data } : WidgetComponentProps<EventsWidgetConfig, EventsListResult>) => {
  const onRenderComplete = useContext(RenderCompletionCallback);

  useEffect(() => {
    onRenderComplete();
  }, [onRenderComplete]);

  return (
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
};

export default EventsNumber;
