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
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import ImmutablePropTypes from 'react-immutable-proptypes';
import _ from 'lodash';
import styled, { css } from 'styled-components';
import { SizeMe } from 'react-sizeme';

import connect from 'stores/connect';
import CustomPropTypes from 'views/components/CustomPropTypes';
import ReactGridContainer from 'components/common/ReactGridContainer';
import { widgetDefinition } from 'views/logic/Widgets';
import { TitlesStore, TitleTypes } from 'views/stores/TitlesStore';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { RowContentStyles } from 'components/graylog/Row';

import { PositionsMap, WidgetDataMap, WidgetErrorsMap, WidgetsMap } from './widgets/WidgetPropTypes';
import WidgetComponent from './WidgetComponent';
import defaultTitle from './defaultTitle';

const DashboardWrap = styled.div(({ theme }) => css`
  color: ${theme.colors.global.textDefault};
  margin: 0;
  width: 100%;
`);

export const WidgetContainer = styled.div`
  z-index: auto;
  ${RowContentStyles}
  height: 100%;
`;

class WidgetGrid extends React.Component {
  static _defaultDimensions(type) {
    const widgetDef = widgetDefinition(type);

    return new WidgetPosition(1, 1, widgetDef.defaultHeight, widgetDef.defaultWidth);
  }

  static propTypes = {
    allFields: CustomPropTypes.FieldListType.isRequired,
    data: WidgetDataMap.isRequired,
    errors: WidgetErrorsMap.isRequired,
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

  constructor(props) {
    super(props);

    this.state = {
      widgetDimensions: {},
    };
  }

  _onWidgetSizeChange = (widgetId, dimensions) => {
    this.setState(({ widgetDimensions }) => ({ widgetDimensions: { ...widgetDimensions, [widgetId]: dimensions } }));
  };

  _renderWidgets = (widgets, positions, data, errors) => {
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
      // eslint-disable-next-line react/destructuring-assignment
      this.props.onPositionsChange(newPositions);
    };

    Object.keys(widgets).forEach((widgetId) => {
      const widget = widgets[widgetId];
      returnedWidgets.positions[widgetId] = positions[widgetId] || WidgetGrid._defaultDimensions(widget.type);

      const { widgetDimensions = {} } = this.state;
      const { fields, allFields, titles = Immutable.Map() } = this.props;
      const widgetTitle = titles.getIn([TitleTypes.Widget, widget.id], defaultTitle(widget));

      returnedWidgets.widgets.push(
        <WidgetContainer key={widgetId}>
          <WidgetComponent widget={widget}
                           widgetId={widgetId}
                           data={data}
                           errors={errors}
                           widgetDimension={widgetDimensions[widgetId] || {}}
                           title={widgetTitle}
                           position={returnedWidgets.positions[widgetId]}
                           onPositionsChange={onPositionsChange}
                           fields={fields}
                           allFields={allFields}
                           onWidgetSizeChange={this._onWidgetSizeChange} />
        </WidgetContainer>,
      );
    });

    return returnedWidgets;
  };

  render() {
    const { staticWidgets, data, errors, locked, onPositionsChange } = this.props;
    // eslint-disable-next-line react/destructuring-assignment
    const { widgets, positions } = this._renderWidgets(this.props.widgets, this.props.positions, data, errors);
    const grid = widgets && widgets.length > 0 ? (
      <ReactGridContainer animate
                          locked={locked}
                          columns={{
                            xxl: 12,
                            xl: 12,
                            lg: 12,
                            md: 12,
                            sm: 12,
                            xs: 12,
                          }}
                          measureBeforeMount
                          onPositionsChange={onPositionsChange}
                          positions={positions}
                          useDragHandle=".widget-drag-handle">
        {widgets}
      </ReactGridContainer>
    ) : <span />;

    // The SizeMe component is required to update the widget grid
    // when its content height results in a scrollbar
    return (
      <SizeMe monitorWidth refreshRate={100}>
        {({ size }) => {
          return (
            <DashboardWrap>
              {React.cloneElement(grid, { width: size.width })}
              {staticWidgets}
            </DashboardWrap>
          );
        }}
      </SizeMe>
    );
  }
}

export default connect(WidgetGrid, { titles: TitlesStore });
