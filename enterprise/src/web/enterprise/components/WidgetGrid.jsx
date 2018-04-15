import React from 'react';
import PropTypes from 'prop-types';
import { Row } from 'react-bootstrap';
import _ from 'lodash';

import connect from 'stores/connect';
import style from 'pages/ShowDashboardPage.css';
import ReactGridContainer from './ReactGridContainer';
import { widgetDefinition } from 'enterprise/logic/Widget';
import Widget from './widgets/Widget';
import { PositionsMap, WidgetsMap, WidgetDataMap } from './widgets/WidgetPropTypes';
import CurrentTitlesStore from '../stores/CurrentTitlesStore';

const defaultTitleGenerator = w => `Unnamed ${w.type.replace('_', ' ').split(' ').map(_.capitalize).join(' ')}`;

class WidgetGrid extends React.Component {
  static _defaultDimensions(type) {
    const widgetDef = widgetDefinition(type);
    return { col: 1, row: 1, height: widgetDef.defaultHeight, width: widgetDef.defaultWidth };
  }

  static _defaultTitle(widget) {
    const widgetDef = widgetDefinition(widget.type);
    return (widgetDef.titleGenerator || defaultTitleGenerator)(widget);
  }

  static propTypes = {
    locked: PropTypes.bool,
    onPositionsChange: PropTypes.func,
    widgets: WidgetsMap.isRequired,
    positions: PositionsMap.isRequired,
    data: WidgetDataMap.isRequired,
  };

  static defaultProps = {
    locked: true,
    positions: {},
    onPositionsChange: () => {
    },
  };

  state = {
    widgetDimensions: {},
  };

  _onWidgetSizeChange = (widgetId, dimensions) => {
    const widgetDimensions = (this.state && this.state.widgetDimensions) || {};
    widgetDimensions[widgetId] = dimensions;
    this.setState({ widgetDimensions: widgetDimensions });
  };

  _renderWidgets = (widgets, positions, data) => {
    const returnedWidgets = { positions: {}, widgets: [] };

    if (!widgets || _.isEmpty(widgets) || !data || _.isEmpty(data)) {
      return returnedWidgets;
    }

    Object.keys(widgets).forEach((widgetId) => {
      const widget = widgets[widgetId];
      const dataKey = widget.data || widgetId;
      const widgetData = data[dataKey];

      returnedWidgets.positions[widgetId] = Object.assign({}, (positions[widgetId] || WidgetGrid._defaultDimensions(widget.type)));

      const { height, width } = (this.state && this.state.widgetDimensions[widgetId]) || {};

      const widgetTitle = this.props.titles.getIn(['widget', widget.id], WidgetGrid._defaultTitle(widget));

      if (widgetData) {
        returnedWidgets.widgets.push(
          <div key={widget.id} className={style.widgetContainer}>
            <Widget key={widgetId}
                    id={widgetId}
                    widget={widget}
                    data={widgetData}
                    height={height}
                    width={width}
                    fields={this.props.fields}
                    onSizeChange={this._onWidgetSizeChange}
                    title={widgetTitle} />
          </div>,
        );
      }
    });

    return returnedWidgets;
  };

  render = () => {
    const { widgets, positions } = this._renderWidgets(this.props.widgets, this.props.positions, this.props.data);
    const grid = widgets && widgets.length > 0 ? (
      <ReactGridContainer locked={this.props.locked}
                          positions={positions}
                          animate={false}
                          useDragHandle=".widget-drag-handle"
                          onPositionsChange={this.props.onPositionsChange}>
        {widgets}
      </ReactGridContainer>
    ) : null;
    return (
      <Row>
        <div className="dashboard">
          {grid}
        </div>
      </Row>
    );
  };
}

export default connect(WidgetGrid, { titles: CurrentTitlesStore });
