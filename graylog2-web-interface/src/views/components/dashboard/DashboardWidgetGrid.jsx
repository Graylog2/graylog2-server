import React from 'react';
import PropTypes from 'prop-types';
import { Row } from 'components/graylog';
import _ from 'lodash';
import Immutable from 'immutable';

import connect from 'stores/connect';
import style from 'pages/ShowDashboardPage.css';
import { ReactGridContainer } from 'components/common';
import { widgetDefinition } from 'views/logic/Widget';
import DashboardWidget from './DashboardWidget';
import { ImmutablePositionsMap, WidgetsMap, WidgetDataMap } from '../widgets/WidgetPropTypes';
import { TitlesStore } from '../../stores/TitlesStore';

const defaultTitleGenerator = w => `Unnamed ${w.type.replace('_', ' ').split(' ').map(_.capitalize).join(' ')}`;

class DashboardWidgetGrid extends React.Component {
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
    onPositionsChange: PropTypes.func.isRequired,
    onWidgetDelete: PropTypes.func.isRequired,
    widgets: WidgetsMap.isRequired,
    positions: ImmutablePositionsMap.isRequired,
    data: WidgetDataMap.isRequired,
    titles: PropTypes.instanceOf(Immutable.Map),
    fields: PropTypes.instanceOf(Immutable.Map),
  };

  static defaultProps = {
    locked: true,
    positions: {},
    titles: new Immutable.Map(),
    fields: new Immutable.Map(),
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

      returnedWidgets.positions[widgetId] = Object.assign({}, (positions[widgetId] || DashboardWidgetGrid._defaultDimensions(widget.type)));

      const { height, width } = (this.state && this.state.widgetDimensions[widgetId]) || {};

      const widgetTitle = this.props.titles.getIn(['widget', widget.id], DashboardWidgetGrid._defaultTitle(widget));

      if (widgetData) {
        returnedWidgets.widgets.push(
          <div key={widget.id} className={style.widgetContainer}>
            <DashboardWidget key={widgetId}
                             id={widgetId}
                             widget={widget}
                             data={widgetData}
                             height={height}
                             width={width}
                             fields={this.props.fields}
                             onSizeChange={this._onWidgetSizeChange}
                             onWidgetDelete={this.props.onWidgetDelete}
                             title={widgetTitle} />
          </div>,
        );
      }
    });

    return returnedWidgets;
  };

  render = () => {
    const { widgets, positions } = this._renderWidgets(this.props.widgets, this.props.positions.toJS(), this.props.data);
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

export default connect(DashboardWidgetGrid, { titles: TitlesStore });
