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

import ReactGridContainer from 'components/common/ReactGridContainer';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import WidgetCard from 'views/components/widgets/WidgetCard';
import WidgetContentSkeleton from 'views/components/widgets/WidgetContentSkeleton';

import type { WelcomeSearchWidgetEntry } from './hooks/useWelcomeSearch';

const COLUMNS = { xxl: 12, xl: 12, lg: 12, md: 12, sm: 12, xs: 12 };

const NOOP = () => {};

type Props = {
  entries: Array<WelcomeSearchWidgetEntry>;
};

/**
 * Mirrors the widget grid of a `WelcomeSearch` with titled skeleton cards while its search is being prepared.
 */
const WelcomeSearchSkeleton = ({ entries }: Props) => (
  <ReactGridContainer
    columns={COLUMNS}
    positions={Object.fromEntries(entries.map(({ widget, position }) => [widget.id, position]))}
    onPositionsChange={NOOP}
    isResizable={false}
    locked>
    {entries.map(({ widget, title }) => (
      <div key={widget.id}>
        <WidgetCard headline={title}>
          <WidgetContentSkeleton
            visualization={widget.config instanceof AggregationWidgetConfig ? widget.config.visualization : undefined}
          />
        </WidgetCard>
      </div>
    ))}
  </ReactGridContainer>
);

export default WelcomeSearchSkeleton;
