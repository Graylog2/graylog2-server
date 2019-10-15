import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';
import DocsHelper from 'util/DocsHelper';

import { Col, Row, Jumbotron } from 'components/graylog';
import { CurrentViewStateActions } from 'views/stores/CurrentViewStateStore';
import { Spinner } from 'components/common';
import { widgetDefinition } from 'views/logic/Widgets';
import DocumentationLink from 'components/support/DocumentationLink';
import WidgetGrid from 'views/components/WidgetGrid';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { PositionsMap, ImmutableWidgetsMap } from './widgets/WidgetPropTypes';

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
    <WidgetGrid allFields={allFields}
                data={data}
                errors={errors}
                fields={fields}
                locked={false}
                onPositionsChange={p => _onPositionsChange(p)}
                positions={positions}
                widgets={widgets} />
  );
};

const EmptyDashboardInfo = () => (
  <Row className="content" style={{ marginRight: 0, marginLeft: 0 }}>
    <Col md={12}>
      <Jumbotron style={{ marginBottom: 0 }}>
        <h2>This dashboard has no widgets yet</h2>
        <p>
          You can create a new widget by selecting a widget type in the left sidebar section &quot;Create&quot;.<br />
          Have a look at the <DocumentationLink page={DocsHelper.PAGES.DASHBOARDS} text="documentation" />, to learn more about the widget creation.
        </p>
      </Jumbotron>
    </Col>
  </Row>
);


const Query = ({ allFields, fields, results, positions, widgetMapping, widgets, queryId }) => {
  if (!widgets || widgets.size === 0) {
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
