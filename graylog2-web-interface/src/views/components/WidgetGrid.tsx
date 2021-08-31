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
import { useCallback, useState, useContext, useMemo } from 'react';
import PropTypes from 'prop-types';
import * as Immutable from 'immutable';
import ImmutablePropTypes from 'react-immutable-proptypes';
import _ from 'lodash';
import styled, { css } from 'styled-components';
import { SizeMe } from 'react-sizeme';
import { WidgetPositions, BackendWidgetPosition } from 'views/types';

import CustomPropTypes from 'views/components/CustomPropTypes';
import ReactGridContainer from 'components/common/ReactGridContainer';
import { widgetDefinition } from 'views/logic/Widgets';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import WidgetFocusContext, { FocusContextState } from 'views/components/contexts/WidgetFocusContext';
import SearchError from 'views/logic/SearchError';
import { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import Widget from 'views/logic/widgets/Widget';
import { TitlesMap } from 'views/stores/TitleTypes';

import WidgetContainer from './WidgetContainer';
import { PositionsMap, WidgetDataMap, WidgetErrorsMap, WidgetsMap } from './widgets/WidgetPropTypes';
import WidgetComponent from './WidgetComponent';

const COLUMNS = {
  xxl: 12,
  xl: 12,
  lg: 12,
  md: 12,
  sm: 12,
  xs: 12,
};

const DashboardWrap = styled.div(({ theme }) => css`
  color: ${theme.colors.global.textDefault};
  margin: 0;
  width: 100%;
  height: 100%;
`);

const StyledReactGridContainer = styled(ReactGridContainer)(({ hasFocusedWidget }) => css`
  height: ${hasFocusedWidget ? '100% !important' : '100%'};
  max-height: ${hasFocusedWidget ? '100%' : 'auto'};
  overflow: ${hasFocusedWidget ? 'hidden' : 'visible'};
  transition: none;
`);

const _defaultDimensions = (type) => {
  const widgetDef = widgetDefinition(type);

  return new WidgetPosition(1, 1, widgetDef.defaultHeight, widgetDef.defaultWidth);
};

type WidgetDimensions = { height: number, width: number };

const _onWidgetSizeChange = (
  widgetDimensions: { [widgetId: string]: WidgetDimensions },
  setWidgetDimensions: (newWidgetDimensions: { [widgetId: string]: WidgetDimensions }) => void,
) => (widgetId: string, dimensions: WidgetDimensions) => {
  setWidgetDimensions({ ...widgetDimensions, [widgetId]: dimensions });
};

type Props = {
  data: { [id: string]: any },
  errors: { [id: string]: undefined | SearchError[] },
  locked?: boolean,
  fields: FieldTypeMappingsList,
  positions: WidgetPositions,
  widgets: { [widgetId: string]: Widget },
  staticWidgets?: React.ReactNode,
  onPositionsChange: (newPositions: Array<BackendWidgetPosition>) => void,
  titles: TitlesMap,
};

type WidgetsProps = Omit<Props, 'staticWidgets' | 'locked' | 'onPositionsChange'> & {
  setWidgetDimensions: (newWidgetDimensions: { [widgetId: string]: WidgetDimensions }) => void,
  widgetDimensions: { [widgetId: string]: WidgetDimensions },
  focusedWidget: FocusContextState | undefined,
  onPositionsChange: (position: BackendWidgetPosition) => void,
};

const renderWidgets = ({
  data,
  errors,
  fields,
  onPositionsChange,
  positions,
  setWidgetDimensions,
  widgetDimensions,
  widgets,
  focusedWidget,
}: WidgetsProps) => {
  if (!widgets || _.isEmpty(widgets) || !data) {
    return [];
  }

  return Object.entries(widgets).map(([widgetId, widget]) => {
    const isFocused = focusedWidget?.id === widgetId && focusedWidget?.focusing;
    const editing = focusedWidget?.id === widgetId && focusedWidget?.editing;

    return (
      <WidgetContainer isFocused={isFocused} key={widget.id}>
        <WidgetComponent data={data}
                         editing={editing}
                         errors={errors}
                         fields={fields}
                         onPositionsChange={onPositionsChange}
                         onWidgetSizeChange={_onWidgetSizeChange(widgetDimensions, setWidgetDimensions)}
                         position={positions[widgetId]}
                         widgetDimension={widgetDimensions[widgetId] || {} as WidgetDimensions}
                         widget={widget} />
      </WidgetContainer>
    );
  });
};

const generatePositions = (widgets: Array<{ id: string, type: string }>, positions: { [widgetId: string]: WidgetPosition }) => widgets
  .map<[string, WidgetPosition]>(({ id, type }) => [id, positions[id] ?? _defaultDimensions(type)])
  .reduce((prev, [id, position]) => ({ ...prev, [id]: position }), {});

const WidgetGrid = ({
  staticWidgets,
  data,
  errors,
  locked,
  onPositionsChange,
  widgets: propsWidgets,
  positions: propsPositions,
  fields,
  titles,
}: Props) => {
  const { focusedWidget } = useContext(WidgetFocusContext);
  const [widgetDimensions, setWidgetDimensions] = useState({});

  const positions = useMemo(() => { return generatePositions(Object.entries(propsWidgets).map(([id, { type }]) => ({ id, type })), propsPositions); }, [propsWidgets, propsPositions]);

  const _onPositionsChange = useCallback((newPosition: BackendWidgetPosition) => {
    const newPositions = Object.keys(positions).map((id) => {
      const { col, row, height, width } = positions[id];

      return { id, col, row, height, width };
    });

    onPositionsChange([...newPositions, newPosition]);
  }, [onPositionsChange, positions]);

  // The SizeMe component is required to update the widget grid
  // when its content height results in a scrollbar
  return (
    <SizeMe monitorWidth refreshRate={100}>
      {({ size: { width } }) => {
        const grid = propsWidgets && Object.keys(propsWidgets).length > 0 ? (
          <StyledReactGridContainer hasFocusedWidget={!!focusedWidget?.id}
                                    columns={COLUMNS}
                                    isResizable={!focusedWidget?.id}
                                    locked={locked}
                                    measureBeforeMount
                                    onPositionsChange={_onPositionsChange}
                                    positions={positions}
                                    width={width}
                                    useDragHandle=".widget-drag-handle">
            {renderWidgets({
              data,
              errors,
              fields,
              onPositionsChange: _onPositionsChange,
              positions,
              widgets: propsWidgets,
              setWidgetDimensions,
              titles,
              widgetDimensions,
              focusedWidget,
            })}
          </StyledReactGridContainer>
        ) : <span />;

        return (
          <DashboardWrap>
            {grid}
            {staticWidgets}
          </DashboardWrap>
        );
      }}
    </SizeMe>
  );
};

WidgetGrid.displayName = 'WidgetGrid';

WidgetGrid.propTypes = {
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

const MemoizedWidgetGrid = React.memo(WidgetGrid);

export default MemoizedWidgetGrid;
