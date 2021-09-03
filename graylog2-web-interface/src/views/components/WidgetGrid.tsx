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
import { useStore } from 'stores/connect';
import { WidgetStore } from 'views/stores/WidgetStore';
import { CurrentViewStateStore } from 'views/stores/CurrentViewStateStore';

import WidgetContainer from './WidgetContainer';
import { PositionsMap, WidgetDataMap, WidgetErrorsMap } from './widgets/WidgetPropTypes';
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

type SharedProps = {
  data: { [id: string]: any },
  errors: { [id: string]: undefined | SearchError[] },
  fields: FieldTypeMappingsList,
  positions: WidgetPositions,
};

type Props = SharedProps & {
  locked?: boolean,
  staticWidgets?: React.ReactNode,
  onPositionsChange: (newPositions: Array<BackendWidgetPosition>) => void,
};

type WidgetsProps = SharedProps & {
  widgetId: string,
  setWidgetDimensions: (newWidgetDimensions: { [widgetId: string]: WidgetDimensions }) => void,
  widgetDimensions: { [widgetId: string]: WidgetDimensions },
  focusedWidget: FocusContextState | undefined,
  onPositionsChange: (position: BackendWidgetPosition) => void,
};

const WidgetGridItem = ({
  data,
  errors,
  fields,
  onPositionsChange,
  positions,
  setWidgetDimensions,
  widgetDimensions,
  widgetId,
  focusedWidget,
}: WidgetsProps) => {
  const editing = focusedWidget?.id === widgetId && focusedWidget?.editing;
  const widgetPosition = positions[widgetId];
  const onWidgetSizeChange = useMemo(() => _onWidgetSizeChange(widgetDimensions, setWidgetDimensions), [widgetDimensions, setWidgetDimensions]);

  return (
    <WidgetComponent data={data}
                     editing={editing}
                     errors={errors}
                     fields={fields}
                     onPositionsChange={onPositionsChange}
                     onWidgetSizeChange={onWidgetSizeChange}
                     position={widgetPosition}
                     widgetDimension={widgetDimensions[widgetId] || {} as WidgetDimensions}
                     widgetId={widgetId} />
  );
};

const generatePositions = (widgets: Array<{ id: string, type: string }>, positions: { [widgetId: string]: WidgetPosition }) => widgets
  .map<[string, WidgetPosition]>(({ id, type }) => [id, positions[id] ?? _defaultDimensions(type)])
  .reduce((prev, [id, position]) => ({ ...prev, [id]: position }), {});

const Grid = ({ children, locked, onPositionsChange }) => {
  const { focusedWidget } = useContext(WidgetFocusContext);

  const initialPositions = useStore(CurrentViewStateStore, (viewState) => viewState?.state?.widgetPositions);
  const widgets = useStore(WidgetStore, (state) => state.map(({ id, type }) => ({ id, type })).toArray());

  const positions = useMemo(() => generatePositions(widgets, initialPositions), [widgets, initialPositions]);

  return (
    <SizeMe monitorWidth refreshRate={100}>
      {({ size: { width } }) => (
        <StyledReactGridContainer hasFocusedWidget={!!focusedWidget?.id}
                                  columns={COLUMNS}
                                  isResizable={!focusedWidget?.id}
                                  locked={locked}
                                  measureBeforeMount
                                  onPositionsChange={onPositionsChange}
                                  positions={positions}
                                  width={width}
                                  useDragHandle=".widget-drag-handle">
          {children}
        </StyledReactGridContainer>
      )}
    </SizeMe>
  );
};

const WidgetGrid = ({
  staticWidgets,
  data,
  errors,
  locked,
  onPositionsChange,
  positions: propsPositions,
  fields,
}: Props) => {
  const { focusedWidget } = useContext(WidgetFocusContext);
  const [widgetDimensions, setWidgetDimensions] = useState({});

  const widgets = useStore(WidgetStore, (state) => state.map(({ id, type }) => ({ id, type })).toArray());

  const positions = useMemo(() => generatePositions(widgets, propsPositions), [widgets, propsPositions]);

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
    <DashboardWrap>
      <Grid locked={locked}
            onPositionsChange={_onPositionsChange}>
        {widgets.map(({ id: widgetId }) => (
          <WidgetContainer key={widgetId} isFocused={focusedWidget?.id === widgetId && focusedWidget?.focusing}>

            <WidgetGridItem data={data}
                            errors={errors}
                            fields={fields}
                            positions={positions}
                            widgetId={widgetId}
                            setWidgetDimensions={setWidgetDimensions}
                            widgetDimensions={widgetDimensions}
                            focusedWidget={focusedWidget}
                            onPositionsChange={_onPositionsChange} />
          </WidgetContainer>
        ))}
      </Grid>
      {staticWidgets}
    </DashboardWrap>
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
};

WidgetGrid.defaultProps = {
  locked: true,
  staticWidgets: [],
  positions: {},
};

const MemoizedWidgetGrid = React.memo(WidgetGrid);

export default MemoizedWidgetGrid;
