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
import React from 'react';
import { useContext } from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';
import styled, { css } from 'styled-components';

import DocsHelper from 'util/DocsHelper';
import { Jumbotron } from 'components/graylog';
import { CurrentViewStateStore, CurrentViewStateActions } from 'views/stores/CurrentViewStateStore';
import { useStore } from 'stores/connect';
import { Spinner } from 'components/common';
import { widgetDefinition } from 'views/logic/Widgets';
import DocumentationLink from 'components/support/DocumentationLink';
import IfDashboard from 'views/components/dashboard/IfDashboard';
import IfSearch from 'views/components/search/IfSearch';
import WidgetGrid, { WidgetContainer } from 'views/components/WidgetGrid';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import WidgetComponent from 'views/components/WidgetComponent';
import { TitlesStore, TitleTypes } from 'views/stores/TitlesStore';
import defaultTitle from 'views/components/defaultTitle';

import { PositionsMap, ImmutableWidgetsMap } from './widgets/WidgetPropTypes';
import InteractiveContext from './contexts/InteractiveContext';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';

const StyledJumbotron = styled(Jumbotron)(({ theme }) => css`
  .container-fluid & {
    border: 1px solid ${theme.colors.gray[80]};
    border-top-left-radius: 0;
    border-top-right-radius: 0;
    margin-bottom: 0;
  }
`);

const MAXIMUM_GRID_SIZE = 12;

const _onPositionsChange = (positions) => {
  const newPositions: Record<string, WidgetPosition> = Immutable.Map<string, WidgetPosition>(
    positions.map(({ col, height, row, width, id }) => [id, new WidgetPosition(col, row, height, width >= MAXIMUM_GRID_SIZE ? Infinity : width)]),
  ).toJS();

  CurrentViewStateActions.widgetPositions(newPositions);
};

const _renderWidgetGrid = (widgetDefs, widgetMapping, results, positions, queryId, fields, allFields, focusedWidget, titles) => {
  const widgets = {};
  const data = {};
  const errors = {};
  const { searchTypes } = results;

  widgetDefs.forEach((widget) => {
    const widgetType = widgetDefinition(widget.type);
    const dataTransformer = widgetType.searchResultTransformer || ((x) => x);
    const searchTypeIds = (widgetMapping[widget.id] || []);
    const widgetData = searchTypeIds.map((searchTypeId) => searchTypes[searchTypeId]).filter((result) => result);
    const widgetErrors = results.errors.filter((e) => searchTypeIds.includes(e.searchTypeId));

    widgets[widget.id] = widget;
    data[widget.id] = dataTransformer(widgetData, widget);

    if (widgetErrors && widgetErrors.length > 0) {
      errors[widget.id] = widgetErrors;
    }

    if (!widgetData || widgetData.length === 0) {
      const queryErrors = results.errors.filter((e) => e.type === 'query');

      if (queryErrors.length > 0) {
        errors[widget.id] = errors[widget.id] ? [].concat(errors[widget.id], queryErrors) : queryErrors;
      }
    }
  });

  if (focusedWidget) {
    const widget = widgets[focusedWidget];
    const title = titles.getIn([TitleTypes.Widget, widget.id], defaultTitle(widget));

    return (
      <WidgetContainer>
        <WidgetComponent widget={widget}
                         widgetId={widget.id}
                         data={data}
                         errors={errors}
                         widgetDimension={{ height: 100, width: 200 }}
                         title={title}
                         position={WidgetPosition.builder().build()}
                         fields={fields}
                         allFields={allFields} />
      </WidgetContainer>
    );
  }

  return (
    <InteractiveContext.Consumer>
      {(interactive) => (
        <WidgetGrid allFields={allFields}
                    data={data}
                    errors={errors}
                    fields={fields}
                    locked={!interactive}
                    onPositionsChange={(p) => _onPositionsChange(p)}
                    positions={positions}
                    widgets={widgets} />
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

const Query = ({ allFields, fields, results, positions, widgetMapping, widgets, queryId }) => {
  const { focusedWidget } = useContext(WidgetFocusContext);
  const titles = useStore(TitlesStore);

  if (!widgets || widgets.isEmpty()) {
    return <EmptyDashboardInfo />;
  }

  if (results) {
    const content = _renderWidgetGrid(
      widgets,
      widgetMapping.toJS(),
      results,
      positions,
      queryId,
      fields,
      allFields,
      focusedWidget,
      titles,
    );

    return (<>{content}</>);
  }

  return <Spinner />;
};

Query.propTypes = {
  allFields: PropTypes.object.isRequired,
  fields: PropTypes.object.isRequired,
  positions: PositionsMap,
  queryId: PropTypes.string.isRequired,
  results: PropTypes.object.isRequired,
  widgetMapping: PropTypes.object.isRequired,
  widgets: ImmutableWidgetsMap.isRequired,
};

Query.defaultProps = {
  positions: {},
};

export default Query;
