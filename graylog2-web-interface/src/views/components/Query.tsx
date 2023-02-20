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
import styled, { css } from 'styled-components';

import DocsHelper from 'util/DocsHelper';
import { Jumbotron } from 'components/bootstrap';
import DocumentationLink from 'components/support/DocumentationLink';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import IfSearch from 'views/components/search/IfSearch';
import WidgetGrid from 'views/components/WidgetGrid';
import useWidgets from 'views/hooks/useWidgets';

const StyledJumbotron = styled(Jumbotron)(({ theme }) => css`
  .container-fluid & {
    border: 1px solid ${theme.colors.gray[80]};
    border-top-left-radius: 0;
    border-top-right-radius: 0;
    margin-bottom: 0;
  }
`);

const NoWidgetsInfo = () => (
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

const useHasWidgets = () => {
  const widgets = useWidgets();

  return widgets?.size > 0;
};

const Query = () => {
  const hasWidgets = useHasWidgets();

  return hasWidgets
    ? <WidgetGrid />
    : <NoWidgetsInfo />;
};

const memoizedQuery = React.memo(Query);
memoizedQuery.displayName = 'Query';

export default memoizedQuery;
