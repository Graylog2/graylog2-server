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
import * as React from 'react';
import { useState, useContext, useMemo } from 'react';
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
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';

import WidgetContainer from './WidgetContainer';
import { PositionsMap, WidgetDataMap, WidgetErrorsMap, WidgetsMap } from './widgets/WidgetPropTypes';
import WidgetComponent from './WidgetComponent';
import defaultTitle from './defaultTitle';

const DashboardWrap = styled.div(({ theme }) => css`
  color: ${theme.colors.global.textDefault};
  margin: 0;
  width: 100%;
  height: 100%;
`);

const StyledReactGridContainer = styled(ReactGridContainer)(({ focusedWidget }) => `
  height: ${focusedWidget ? '100% !important' : '100%'};
  max-height: ${focusedWidget ? '100%' : 'auto'};
  overflow: ${focusedWidget ? 'hidden' : 'visible'};
  transition: none;
`);

const _defaultDimensions = (type) => {
  const widgetDef = widgetDefinition(type);

  return new WidgetPosition(1, 1, widgetDef.defaultHeight, widgetDef.defaultWidth);
};

const _onWidgetSizeChange = (widgetDimensions, setWidgetDimensions) => (widgetId, dimensions) => {
  setWidgetDimensions({ ...widgetDimensions, [widgetId]: dimensions });
};

const _renderWidgets = ({
  allFields,
  data,
  errors,
  fields,
  onPositionsChange,
  positions,
  setWidgetDimensions,
  titles,
  widgetDimensions,
  widgets,
  focusedWidget,
}) => {
  const returnedWidgets = { positions: {}, widgets: [] };

  if (!widgets || _.isEmpty(widgets) || !data) {
    return returnedWidgets;
  }

  const _onPositionsChange = (newPosition) => {
    const newPositions = Object.keys(positions).map((id) => {
      const { col, row, height, width } = positions[id]._value;

      return { id: id, col: col, row: row, height: height, width: width };
    });

    newPositions.push(newPosition);
    // eslint-disable-next-line react/destructuring-assignment
    onPositionsChange(newPositions);
  };

  Object.keys(widgets).forEach((widgetId) => {
    const widget = widgets[widgetId];
    returnedWidgets.positions[widgetId] = positions[widgetId] || _defaultDimensions(widget.type);
    const widgetTitle = titles.getIn([TitleTypes.Widget, widget.id], defaultTitle(widget));
    const isFocused = focusedWidget === widgetId;

    returnedWidgets.widgets.push(
      <WidgetContainer isFocused={isFocused} key={widget.id}>
        <WidgetComponent allFields={allFields}
                         data={data}
                         errors={errors}
                         fields={fields}
                         onPositionsChange={_onPositionsChange}
                         onWidgetSizeChange={_onWidgetSizeChange(widgetDimensions, setWidgetDimensions)}
                         position={returnedWidgets.positions[widgetId]}
                         title={widgetTitle}
                         widgetDimension={widgetDimensions[widgetId] || {}}
                         widget={widget} />
      </WidgetContainer>,
    );
  });

  return returnedWidgets;
};

const WidgetGrid = ({
  staticWidgets,
  data,
  errors,
  locked,
  onPositionsChange,
  widgets: propsWidgets,
  positions: propsPositions,
  fields,
  allFields,
  titles,
}) => {
  const { focusedWidget } = useContext(WidgetFocusContext);
  const [widgetDimensions, setWidgetDimensions] = useState({});

  const { widgets, positions } = useMemo(() => _renderWidgets({
    allFields,
    data,
    errors,
    fields,
    onPositionsChange,
    positions: propsPositions,
    widgets: propsWidgets,
    setWidgetDimensions,
    titles,
    widgetDimensions,
    focusedWidget,
  }), [
    allFields,
    data,
    errors,
    fields,
    onPositionsChange,
    propsPositions,
    propsWidgets,
    setWidgetDimensions,
    titles,
    widgetDimensions,
    focusedWidget,
  ]);

  const grid = widgets && widgets.length > 0 ? (
    <StyledReactGridContainer focusedWidget={focusedWidget}
                              columns={{
                                xxl: 12,
                                xl: 12,
                                lg: 12,
                                md: 12,
                                sm: 12,
                                xs: 12,
                              }}
                              isResizable={!focusedWidget}
                              locked={locked}
                              measureBeforeMount
                              onPositionsChange={onPositionsChange}
                              positions={positions}
                              useDragHandle=".widget-drag-handle">
      {widgets}
    </StyledReactGridContainer>
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
};

WidgetGrid.propTypes = {
  allFields: CustomPropTypes.FieldListType.isRequired,
  data: WidgetDataMap.isRequired,
  errors: WidgetErrorsMap.isRequired,
  fields: CustomPropTypes.FieldListType.isRequired,
  locked: PropTypes.bool,
  onPositionsChange: PropTypes.func.isRequired,
  positions: PositionsMap,
  staticWidgets: PropTypes.arrayOf(PropTypes.node),
  titles: ImmutablePropTypes.map,
  widgets: WidgetsMap.isRequired,
};

WidgetGrid.defaultProps = {
  locked: true,
  staticWidgets: [],
  positions: {},
  titles: Immutable.Map(),
};

export default connect(WidgetGrid, { titles: TitlesStore });
