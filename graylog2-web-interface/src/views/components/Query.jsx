import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';
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
import { PositionsMap, ImmutableWidgetsMap } from './widgets/WidgetPropTypes';
import InteractiveContext from './contexts/InteractiveContext';

const MAXIMUM_GRID_SIZE = 12;

const _onPositionsChange = (positions) => {
  const newPositions = Immutable.Map(positions.map(({ col, height, row, width, id }) => [id, new WidgetPosition(col, row, height, width >= MAXIMUM_GRID_SIZE ? Infinity : width)])).toJS();
  CurrentViewStateActions.widgetPositions(newPositions);
};

const _renderWidgetGrid = (widgetDefs, widgetMapping, results, positions, queryId, fields, allFields) => {
  const widgets = {};
  const data = {};
  const errors = {};
  const { searchTypes } = results;

  widgetDefs.forEach((widget) => {
    const widgetType = widgetDefinition(widget.type);
    const dataTransformer = widgetType.searchResultTransformer || (x => x);
    const searchTypeIds = (widgetMapping[widget.id] || []);
    const widgetData = searchTypeIds.map(searchTypeId => searchTypes[searchTypeId]).filter(result => result);
    const widgetErrors = results.errors.filter(e => searchTypeIds.includes(e.searchTypeId));

    widgets[widget.id] = widget;
    data[widget.id] = dataTransformer(widgetData, widget);

    if (widgetErrors && widgetErrors.length > 0) {
      errors[widget.id] = widgetErrors;
    }

    if (!widgetData || widgetData.length === 0) {
      const queryErrors = results.errors.filter(e => e.type === 'query');
      if (queryErrors.length > 0) {
        errors[widget.id] = errors[widget.id] ? [].concat(errors[widget.id], queryErrors) : queryErrors;
      }
    }
  });
  return (
    <InteractiveContext.Consumer>
      {interactive => (
        <WidgetGrid allFields={allFields}
                    data={data}
                    errors={errors}
                    fields={fields}
                    locked={!interactive}
                    onPositionsChange={p => _onPositionsChange(p)}
                    positions={positions}
                    widgets={widgets} />
      )}
    </InteractiveContext.Consumer>
  );
};

const EmptyDashboardInfo = () => (
  <Jumbotron style={{ marginBottom: 0 }}>
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
  </Jumbotron>
);


const Query = ({ allFields, fields, results, positions, widgetMapping, widgets, queryId }) => {
  if (!widgets || widgets.isEmpty()) {
    return <EmptyDashboardInfo />;
  }

  if (results) {
    const content = _renderWidgetGrid(widgets, widgetMapping.toJS(), results, positions, queryId, fields, allFields);
    return (<span>{content}</span>);
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
