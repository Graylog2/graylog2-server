import React from 'react';
import PropTypes from 'prop-types';
import { Col } from 'react-bootstrap';

import { Spinner } from 'components/common';

import { widgetDefinition } from 'enterprise/logic/Widget';
import ViewsActions from 'enterprise/actions/ViewsActions';

import WidgetGrid from 'enterprise/components/WidgetGrid';

const _onPositionsChange = (positions, view, queryId) => {
  const newPositions = {};
  positions.forEach(({ col, height, row, width, id }) => {
    newPositions[id] = { col, height, row, width };
  });
  const updatedView = view.update('positions', (p) => {
    p[queryId] = newPositions;
    return p;
  });
  ViewsActions.update(updatedView.get('id'), updatedView);
};

const _renderWidgetGrid = (widgetDefs, widgetMapping, searchTypes, view, queryId, fields, allFields) => {
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
  const positions = view.get('positions')[queryId];
  return (
    <WidgetGrid fields={fields}
                allFields={allFields}
                locked={false}
                widgets={widgets}
                positions={positions}
                data={data}
                onPositionsChange={p => _onPositionsChange(p, view, queryId)} />
  );
};

const Query = ({ children, allFields, fields, results, view, widgetMapping, widgets, query }) => {
  if (results) {
    const queryId = query.get('id');
    const widgetGrid = _renderWidgetGrid(widgets, widgetMapping, results.searchTypes, view, queryId, fields, allFields, );
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
  view: PropTypes.object.isRequired,
  widgetMapping: PropTypes.object.isRequired,
  widgets: PropTypes.object.isRequired,
  query: PropTypes.object.isRequired,
};

export default Query;
