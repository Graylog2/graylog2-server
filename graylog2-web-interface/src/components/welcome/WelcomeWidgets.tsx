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
import { ReplaySearchButtonComponent } from 'views/components/widgets/ReplaySearchButton';
import StandaloneWidgetGroup from 'views/components/standalone/StandaloneWidgetGroup';
import StandaloneWidgetCard from 'views/components/standalone/StandaloneWidgetCard';

import WelcomeWidgetsSkeleton, { WIDGET_GRID_COLUMNS } from './WelcomeWidgetsSkeleton';
import type { WelcomeWidgetEntry } from './welcomeWidgetBuilders';

const NOOP = () => {};

type Props = {
  entries: Array<WelcomeWidgetEntry>;
};

/**
 * Shows welcome-page widgets read-only in a fixed grid, running one search for all of them.
 * The entries need stable widget ids, otherwise the search runs again on every render.
 */
const WelcomeWidgets = ({ entries }: Props) => (
  <StandaloneWidgetGroup
    widgets={entries.map(({ widget }) => widget)}
    titles={Object.fromEntries(entries.map(({ widget, title }) => [widget.id, title]))}
    loadingPlaceholder={<WelcomeWidgetsSkeleton entries={entries} />}>
    <ReactGridContainer
      columns={WIDGET_GRID_COLUMNS}
      positions={Object.fromEntries(entries.map(({ widget, position }) => [widget.id, position]))}
      onPositionsChange={NOOP}
      isResizable={false}
      locked>
      {entries.map(({ widget }) => (
        <div key={widget.id}>
          <StandaloneWidgetCard
            widgetId={widget.id}
            actions={widget.context && <ReplaySearchButtonComponent searchLink={widget.context} />}
          />
        </div>
      ))}
    </ReactGridContainer>
  </StandaloneWidgetGroup>
);

export default WelcomeWidgets;
