import React from 'react';
import PropTypes from 'prop-types';
import { Row } from 'react-bootstrap';
import { PluginStore } from 'graylog-web-plugin/plugin';
import _ from 'lodash';

import { ReactGridContainer } from 'components/common';
import style from 'pages/ShowDashboardPage.css';
import ViewWidget from 'enterprise/components/widgets/ViewWidget';

export default class WidgetGrid extends React.Component {
  static _defaultDimensions(type) {
    const widgetDefinition = PluginStore.exports('widgets').filter(widget => widget.type.toUpperCase() === type.toUpperCase())[0];
    return { col: 1, row: 1, height: widgetDefinition.defaultHeight, width: widgetDefinition.defaultWidth };
  }

  static _visualizationForType(type) {
    return PluginStore.exports('widgets').filter(widget => widget.type.toUpperCase() === type.toUpperCase())[0].visualizationComponent;
  }

  static propTypes = {
    locked: PropTypes.bool,
    onPositionsChange: PropTypes.func,
    widgets: PropTypes.object.isRequired,
  };

  static defaultProps = {
    locked: true,
    positions: {},
    onPositionsChange: () => {},
  };

  state = {
    widgetDimensions: {},
  };

  _onWidgetSizeChange = (widgetId, dimensions) => {
    const widgetDimensions = (this.state && this.state.widgetDimensions) || {};
    widgetDimensions[widgetId] = dimensions;
    this.setState({ widgetDimensions: widgetDimensions });
  };

  _renderWidgets = (widgetConfig) => {
    const returnedWidgets = { positions: {}, widgets: [] };

    if (!widgetConfig || _.isEmpty(widgetConfig)) {
      return returnedWidgets;
    }

    const { data, widgets, positions } = widgetConfig;

    Object.keys(widgets).forEach((widgetId) => {
      const widget = widgets[widgetId];
      const VisComponent = WidgetGrid._visualizationForType(widget.type);
      const { config, computationTimeRange } = widget;
      const dataKey = widget.data || widgetId;
      const widgetData = data[dataKey];

      positions[widgetId] = positions[widgetId] || WidgetGrid._defaultDimensions(widget.type);

      const { height, width } = (this.state && this.state.widgetDimensions[widgetId]) || {};

      if (widgetData) {
        returnedWidgets.widgets.push(
          <div key={widgetId} className={style.widgetContainer}>
            <ViewWidget title={widget.title} widgetId={widgetId} onSizeChange={this._onWidgetSizeChange}>
              <VisComponent id={widgetId}
                            config={config}
                            data={widgetData}
                            height={height}
                            width={width}
                            computationTimeRange={computationTimeRange} />
            </ViewWidget>
          </div>,
        );
      }
    });

    returnedWidgets.positions = positions;

    return returnedWidgets;
  };

  render = () => {
    const { widgets, positions } = this._renderWidgets(this.props.widgets);
    const grid = widgets && widgets.length > 0 ? (
      <ReactGridContainer locked={this.props.locked}
                          positions={positions}
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
  }
}
