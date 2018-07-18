import React from 'react';
import PropTypes from 'prop-types';
import Immutable from 'immutable';
import { Col } from 'react-bootstrap';

import { Spinner } from 'components/common';

import { widgetDefinition } from 'enterprise/logic/Widget';
import WidgetGrid from 'enterprise/components/WidgetGrid';
import WidgetPosition from 'enterprise/logic/widgets/WidgetPosition';
import { CurrentViewStateActions } from 'enterprise/stores/CurrentViewStateStore';
import StaticMessageList from './messagelist/StaticMessageList';
import { PositionsMap, ImmutableWidgetsMap } from './widgets/WidgetPropTypes';
import EmptySearchResult from './EmptySearchResult';

const _onPositionsChange = (positions) => {
  const newPositions = Immutable.Map(positions.map(({ col, height, row, width, id }) => [id, new WidgetPosition(col, row, height, width)])).toJS();
  CurrentViewStateActions.widgetPositions(newPositions);
};

const _renderWidgetGrid = (widgetDefs, widgetMapping, searchTypes, positions, queryId, fields, allFields, staticWidgets) => {
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
    <WidgetGrid allFields={allFields}
                data={data}
                fields={fields}
                locked={false}
                onPositionsChange={p => _onPositionsChange(p)}
                positions={positions}
                staticWidgets={staticWidgets}
                widgets={widgets} />
  );
};

const _extractMessages = (searchTypes) => {
  return new Immutable.Map(searchTypes).find(searchType => searchType.type.toLocaleUpperCase() === 'MESSAGES');
};

const Query = ({ children, allFields, fields, onToggleMessages, results, positions, showMessages, widgetMapping, widgets, queryId }) => {
  if (results) {
    let content;
    if (results.documentCount === 0) {
      content = <EmptySearchResult />;
    } else {
      const staticWidgets = [
        <StaticMessageList key="staticMessageList"
                           messages={results.messages}
                           onToggleMessages={onToggleMessages}
                           showMessages={showMessages} />,
      ];
      content = _renderWidgetGrid(widgets, widgetMapping.toJS(), results.searchTypes, positions, queryId, fields, allFields, staticWidgets);
    }
    return (
      <span>
        <Col md={3} style={{ paddingLeft: 0, paddingRight: 10 }}>
          {children}
        </Col>
        <Col md={9}>
          {content}
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
  onToggleMessages: PropTypes.func.isRequired,
  positions: PositionsMap,
  queryId: PropTypes.string.isRequired,
  results: PropTypes.object.isRequired,
  showMessages: PropTypes.bool.isRequired,
  widgetMapping: PropTypes.object.isRequired,
  widgets: ImmutableWidgetsMap.isRequired,
};

Query.defaultProps = {
  positions: {},
};

export default Query;
