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

import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';
import WidgetCard from 'views/components/widgets/WidgetCard';
import WidgetContentSkeleton from 'views/components/widgets/WidgetContentSkeleton';

import WelcomeWidgetsGrid from './WelcomeWidgetsGrid';
import type { WelcomeWidgetEntry } from './welcomeWidgetBuilders';

type Props = {
  entries: Array<WelcomeWidgetEntry>;
};

/**
 * Mirrors the widget grid of `WelcomeWidgets` with titled skeleton cards while its search is being prepared.
 */
const WelcomeWidgetsSkeleton = ({ entries }: Props) => (
  <WelcomeWidgetsGrid
    entries={entries}
    renderWidget={({ widget, title }) => (
      <WidgetCard headline={title}>
        <WidgetContentSkeleton
          visualization={widget.config instanceof AggregationWidgetConfig ? widget.config.visualization : undefined}
        />
      </WidgetCard>
    )}
  />
);

export default WelcomeWidgetsSkeleton;
