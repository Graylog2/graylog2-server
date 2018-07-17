import React from 'react';
import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';
import { Row } from 'react-bootstrap';
import _ from 'lodash';

import connect from 'stores/connect';
import CustomPropTypes from 'enterprise/components/CustomPropTypes';
import style from 'pages/ShowDashboardPage.css';
import ReactGridContainer from 'components/common/ReactGridContainer';
import { widgetDefinition } from 'enterprise/logic/Widget';
import Widget from './widgets/Widget';
import { PositionsMap, WidgetsMap, WidgetDataMap } from './widgets/WidgetPropTypes';
import { TitlesStore } from '../stores/TitlesStore';
import WidgetPosition from '../logic/widgets/WidgetPosition';

const defaultTitleGenerator = w => `Unnamed ${w.type.replace('_', ' ').split(' ').map(_.capitalize).join(' ')}`;

class WidgetGrid extends React.Component {
  static _defaultDimensions(type) {
    const widgetDef = widgetDefinition(type);
    return new WidgetPosition(1, 1, widgetDef.defaultHeight, widgetDef.defaultWidth);
  }

  static _defaultTitle(widget) {
    const widgetDef = widgetDefinition(widget.type);
    return (widgetDef.titleGenerator || defaultTitleGenerator)(widget);
  }

  static propTypes = {
    allFields: CustomPropTypes.FieldListType.isRequired,
    data: WidgetDataMap.isRequired,
    fields: CustomPropTypes.FieldListType.isRequired,
    locked: PropTypes.bool,
    onPositionsChange: PropTypes.func.isRequired,
    positions: PositionsMap,
    staticWidgets: PropTypes.arrayOf(PropTypes.node),
    titles: ImmutablePropTypes.map.isRequired,
    widgets: WidgetsMap.isRequired,
  };

  static defaultProps = {
    locked: true,
    staticWidgets: [],
    positions: {},
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

    if (!widgets || _.isEmpty(widgets) || !data) {
      return returnedWidgets;
    }

    const onPositionsChange = (newPosition) => {
      const newPositions = Object.keys(positions).map((id) => {
        const { col, row, height, width } = positions[id]._value;
        return { id: id, col: col, row: row, height: height, width: width };
      });
      newPositions.push(newPosition);
      this.props.onPositionsChange(newPositions);
    };

    Object.keys(widgets).forEach((widgetId) => {
      const widget = widgets[widgetId];
      const dataKey = widget.data || widgetId;
      const widgetData = data[dataKey];

      returnedWidgets.positions[widgetId] = positions[widgetId] || WidgetGrid._defaultDimensions(widget.type);

      const { height, width } = (this.state && this.state.widgetDimensions[widgetId]) || {};

      const widgetTitle = this.props.titles.getIn(['widget', widget.id], WidgetGrid._defaultTitle(widget));

      returnedWidgets.widgets.push(
        <div key={widget.id} className={style.widgetContainer}>
          <Widget key={widgetId}
                  id={widgetId}
                  widget={widget}
                  data={widgetData}
                  height={height}
                  position={returnedWidgets.positions[widgetId]}
                  width={width}
                  allFields={this.props.allFields}
                  fields={this.props.fields}
                  onPositionsChange={onPositionsChange}
                  onSizeChange={this._onWidgetSizeChange}
                  title={widgetTitle} />
        </div>,
      );
    });

    return returnedWidgets;
  };

  render() {
    const { staticWidgets } = this.props;
    const { widgets, positions } = this._renderWidgets(this.props.widgets, this.props.positions, this.props.data);
    const grid = widgets && widgets.length > 0 ? (
      <ReactGridContainer animate={false}
                          locked={this.props.locked}
                          measureBeforeMount
                          onPositionsChange={this.props.onPositionsChange}
                          positions={positions}
                          useDragHandle=".widget-drag-handle">
        {widgets}
      </ReactGridContainer>
    ) : null;
    return (
      <Row>
        <div className="dashboard" style={{ marginLeft: '-20px' }}>
          {grid}
          {staticWidgets}
        </div>
      </Row>
    );
  }
}

export default connect(WidgetGrid, { titles: TitlesStore });
