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
import Immutable from 'immutable';
import styled, { css } from 'styled-components';
import { BackendWidgetPosition } from 'views/types';

import DocsHelper from 'util/DocsHelper';
import { Jumbotron } from 'components/graylog';
import { CurrentViewStateActions } from 'views/stores/CurrentViewStateStore';
import DocumentationLink from 'components/support/DocumentationLink';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import IfSearch from 'views/components/search/IfSearch';
import WidgetGrid from 'views/components/WidgetGrid';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { useStore } from 'stores/connect';
import { WidgetStore } from 'views/stores/WidgetStore';

import InteractiveContext from './contexts/InteractiveContext';

const StyledJumbotron = styled(Jumbotron)(({ theme }) => css`
  .container-fluid & {
    border: 1px solid ${theme.colors.gray[80]};
    border-top-left-radius: 0;
    border-top-right-radius: 0;
    margin-bottom: 0;
  }
`);

const MAXIMUM_GRID_SIZE = 12;

const _onPositionsChange = (positions: Array<BackendWidgetPosition>) => {
  const newPositions = Immutable.Map<string, WidgetPosition>(
    positions.map(({ col, height, row, width, id }) => [id, new WidgetPosition(col, row, height, width >= MAXIMUM_GRID_SIZE ? Infinity : width)]),
  ).toObject();

  CurrentViewStateActions.widgetPositions(newPositions);
};

const RenderedWidgetGrid = () => (
  <InteractiveContext.Consumer>
    {(interactive) => (
      <WidgetGrid locked={!interactive}
                  onPositionsChange={_onPositionsChange} />
    )}
  </InteractiveContext.Consumer>
);

const EmptyDashboardInfo = () => (
  <StyledJumbotron>
    <h2>
      <IfDashboard>
        This dashboard has no widgets yet
      </IfDashboard>
      <IfSearch>
        There are no widgets defined to visualize the search result
      </IfSearch>
    </h2>
    <br />
    <p>
      Create a new widget by selecting a widget type in the left sidebar section &quot;Create&quot;.<br />
    </p>
    <p>
      A few tips for creating searches and dashboards
    </p>
    <ul>
      <li><p>1. Start with a <b>question</b> you want to answer. Define the problem you want to solve.</p></li>
      <li><p>2. <b>Limit</b> the data to only the data points you want to see.</p></li>
      <li><p>3. <b>Visualize</b> the data. Does it answer your question?</p></li>
      <IfDashboard>
        <li><p>4. <b>Share</b> the dashboard with your colleagues. Prepare it for <b>reuse</b> by using parameters (contained in <a href="https://www.graylog.org/graylog-enterprise-edition" target="_blank" rel="noopener noreferrer">Graylog Enterprise</a>).</p></li>
      </IfDashboard>
    </ul>
    <p>
      You can also have a look at the <DocumentationLink page={DocsHelper.PAGES.DASHBOARDS} text="documentation" />, to learn more about the widget creation.
    </p>
  </StyledJumbotron>
);

const useWidgetCount = () => useStore(WidgetStore, (widgets) => widgets?.size ?? 0);

const Query = () => {
  const widgetCount = useWidgetCount();

  return widgetCount > 0
    ? <RenderedWidgetGrid />
    : <EmptyDashboardInfo />;
};

const memoizedQuery = React.memo(Query);
memoizedQuery.displayName = 'Query';

export default memoizedQuery;
