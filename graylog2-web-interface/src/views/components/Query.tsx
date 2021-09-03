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
import PropTypes from 'prop-types';
import Immutable from 'immutable';
import styled, { css } from 'styled-components';
import { BackendWidgetPosition } from 'views/types';

import DocsHelper from 'util/DocsHelper';
import { Jumbotron } from 'components/graylog';
import { CurrentViewStateActions } from 'views/stores/CurrentViewStateStore';
import { Spinner } from 'components/common';
import { widgetDefinition } from 'views/logic/Widgets';
import DocumentationLink from 'components/support/DocumentationLink';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import IfSearch from 'views/components/search/IfSearch';
import WidgetGrid from 'views/components/WidgetGrid';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import Widget from 'views/logic/widgets/Widget';
import QueryResult from 'views/logic/QueryResult';
import { WidgetMapping } from 'views/logic/views/types';
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

const _getDataAndErrors = (widget, widgetMapping, results) => {
  const { searchTypes } = results;
  const widgetType = widgetDefinition(widget.type);
  const dataTransformer = widgetType.searchResultTransformer || ((x) => x);
  const searchTypeIds = widgetMapping[widget.id] ?? [];
  const widgetData = searchTypeIds.map((searchTypeId) => searchTypes[searchTypeId]).filter((result) => result);
  const widgetErrors = results.errors.filter((e) => searchTypeIds.includes(e.searchTypeId));
  let error;

  const data = dataTransformer(widgetData, widget);

  if (widgetErrors && widgetErrors.length > 0) {
    error = widgetErrors;
  }

  if (!widgetData || widgetData.length === 0) {
    const queryErrors = results.errors.filter((e) => e.type === 'query');

    if (queryErrors.length > 0) {
      error = error ? [].concat(error, queryErrors) : queryErrors;
    }
  }

  return { widgetData: data, error };
};

type GridProps = {
  widgetDefs: Immutable.Map<string, Widget>,
  widgetMapping: { [widgetId: string]: Array<string> },
  results: QueryResult,
  fields: FieldTypeMappingsList,
}

const RenderedWidgetGrid = ({ widgetDefs, widgetMapping, results, fields }: GridProps) => {
  const data = {};
  const errors = {};

  // eslint-disable-next-line react/destructuring-assignment
  widgetDefs.forEach((widget) => {
    const { widgetData, error } = _getDataAndErrors(widget, widgetMapping, results);

    data[widget.id] = widgetData;
    errors[widget.id] = error;
  });

  return (
    <InteractiveContext.Consumer>
      {(interactive) => (
        <WidgetGrid data={data}
                    errors={errors}
                    fields={fields}
                    locked={!interactive}
                    onPositionsChange={_onPositionsChange} />
      )}
    </InteractiveContext.Consumer>
  );
};

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

type Props = {
  fields: FieldTypeMappingsList,
  results: QueryResult,
  widgetMapping: WidgetMapping,
};

const Query = ({ fields, results, widgetMapping }: Props) => {
  const widgets = useStore(WidgetStore);

  if (!widgets || widgets.isEmpty()) {
    return <EmptyDashboardInfo />;
  }

  return results
    ? <RenderedWidgetGrid widgetDefs={widgets} widgetMapping={widgetMapping.toJS()} results={results} fields={fields} />
    : <Spinner />;
};

Query.propTypes = {
  fields: PropTypes.object.isRequired,
  results: PropTypes.object.isRequired,
  widgetMapping: PropTypes.object.isRequired,
};

const memoizedQuery = React.memo(Query);
memoizedQuery.displayName = 'Query';

export default memoizedQuery;
