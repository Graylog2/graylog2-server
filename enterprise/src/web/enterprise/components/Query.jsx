import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';
import { Col } from 'react-bootstrap';

import { Spinner } from 'components/common';

import { widgetDefinition } from 'enterprise/logic/Widget';
import WidgetGrid from 'enterprise/components/WidgetGrid';
import { CurrentViewStateActions } from 'enterprise/stores/CurrentViewStateStore';

const _onPositionsChange = (positions) => {
  const newPositions = Immutable.Map(positions.map(({ col, height, row, width, id }) => [id, { col, height, row, width }])).toJS();
  CurrentViewStateActions.widgetPositions(newPositions);
};

const _renderWidgetGrid = (widgetDefs, widgetMapping, searchTypes, positions, queryId, fields, allFields) => {
  const widgets = {};
  const data = {};

  widgetDefs.forEach((widget) => {
    const widgetType = widgetDefinition(widget.type);
    const dataTransformer = widgetType.searchResultTransformer || (x => x);
    const widgetData = (widgetMapping[widget.id] || []).map(searchTypeId => searchTypes[searchTypeId]);
    if (widgetData) {
      widgets[widget.id] = widget;
      data[widget.id] = dataTransformer(widgetData, widget);
    }
  });
  return (
    <WidgetGrid fields={fields}
                allFields={allFields}
                locked={false}
                widgets={widgets}
                positions={positions}
                data={data}
                onPositionsChange={p => _onPositionsChange(p)} />
  );
};

const Query = ({ children, allFields, fields, results, positions, widgetMapping, widgets, queryId }) => {
  if (results) {
    const widgetGrid = _renderWidgetGrid(widgets, widgetMapping.toJS(), results.searchTypes, positions, queryId, fields, allFields, );
    return (
      <span>
        <Col md={3} style={{ paddingLeft: 0, paddingRight: 10 }}>
          {children}
        </Col>
        <Col md={9}>
          {widgetGrid}
        </Col>
      </span>
    );
  }

  return <Spinner />;
};

Query.propTypes = {
  allFields: PropTypes.object.isRequired,
  children: PropTypes.node.isRequired,
  fields: PropTypes.object.isRequired,
  results: PropTypes.object.isRequired,
  positions: PropTypes.object.isRequired,
  widgetMapping: PropTypes.object.isRequired,
  widgets: PropTypes.object.isRequired,
  queryId: PropTypes.string.isRequired,
};

export default Query;
