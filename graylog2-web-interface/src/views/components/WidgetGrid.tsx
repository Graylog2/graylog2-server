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
import styled, { css } from 'styled-components';
import { SizeMe } from 'react-sizeme';
import { WidgetPositions, BackendWidgetPosition } from 'views/types';

import ReactGridContainer from 'components/common/ReactGridContainer';
import { widgetDefinition } from 'views/logic/Widgets';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import WidgetFocusContext, { FocusContextState } from 'views/components/contexts/WidgetFocusContext';
import { FieldTypeMappingsList } from 'views/stores/FieldTypesStore';
import { useStore } from 'stores/connect';
import { WidgetStore } from 'views/stores/WidgetStore';
import { CurrentViewStateActions } from 'views/stores/CurrentViewStateStore';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import { ViewMetadataStore } from 'views/stores/ViewMetadataStore';
import { StoreState } from 'stores/StoreTypes';
import { ViewStatesStore } from 'views/stores/ViewStatesStore';

import WidgetContainer from './WidgetContainer';
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

type WidgetsProps = {
  fields: FieldTypeMappingsList,
  widgetId: string,
  setWidgetDimensions: (newWidgetDimensions: { [widgetId: string]: WidgetDimensions }) => void,
  widgetDimensions: { [widgetId: string]: WidgetDimensions },
  focusedWidget: FocusContextState | undefined,
  onPositionsChange: (position: BackendWidgetPosition) => void,
  positions: WidgetPositions,
};

const WidgetGridItem = ({
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
    <WidgetComponent editing={editing}
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

const mapWidgetPositions = (states: StoreState<typeof ViewStatesStore>) => states.map((state) => state.widgetPositions).reduce((prev, cur) => ({ ...prev, ...cur }), {});
const mapWidgets = (state: StoreState<typeof WidgetStore>) => state.map(({ id, type }) => ({ id, type })).toArray();

const useWidgetPositions = () => {
  const initialPositions = useStore(ViewStatesStore, mapWidgetPositions);
  const widgets = useStore(WidgetStore, mapWidgets);

  return useMemo(() => generatePositions(widgets, initialPositions), [widgets, initialPositions]);
};

type GridProps = {
  children: React.ReactNode,
  locked: boolean,
  onPositionsChange: (newPositions: Array<BackendWidgetPosition>) => void,
};

const Grid = ({ children, locked, onPositionsChange }: GridProps) => {
  const { focusedWidget } = useContext(WidgetFocusContext);

  return (
    <SizeMe monitorWidth refreshRate={100}>
      {({ size: { width } }) => (
        <StyledReactGridContainer hasFocusedWidget={!!focusedWidget?.id}
                                  columns={COLUMNS}
                                  isResizable={!focusedWidget?.id}
                                  locked={locked}
                                  measureBeforeMount
                                  onPositionsChange={onPositionsChange}
                                  width={width}
                                  useDragHandle=".widget-drag-handle">
          {children}
        </StyledReactGridContainer>
      )}
    </SizeMe>
  );
};

const useQueryFieldTypes = () => {
  const fieldTypes = useContext(FieldTypesContext);
  const queryId = useStore(ViewMetadataStore, (viewMetadataStore) => viewMetadataStore.activeQuery);

  return useMemo(() => fieldTypes.queryFields.get(queryId, fieldTypes.all), [fieldTypes.all, fieldTypes.queryFields, queryId]);
};

const MAXIMUM_GRID_SIZE = 12;

const convertPosition = ({ col, row, height, width }: BackendWidgetPosition) => new WidgetPosition(col, row, height, width >= MAXIMUM_GRID_SIZE ? Infinity : width);

const onPositionChange = (newPosition: BackendWidgetPosition) => {
  const { id } = newPosition;
  const widgetPosition = convertPosition(newPosition);
  CurrentViewStateActions.updateWidgetPosition(id, widgetPosition);
};

const onPositionsChange = (newPositions: Array<BackendWidgetPosition>) => {
  const widgetPositions = Object.fromEntries(newPositions.map((newPosition) => [newPosition.id, convertPosition(newPosition)]));
  CurrentViewStateActions.widgetPositions(widgetPositions);
};

const mapPosition = (id, { col, row, height, width }) => ({
  i: id,
  x: col ? Math.max(col - 1, 0) : 0,
  y: (row === undefined || row <= 0 ? Infinity : row - 1),
  h: height || 1,
  w: width || 1,
});

const WidgetGrid = () => {
  const isInteractive = useContext(InteractiveContext);
  const { focusedWidget } = useContext(WidgetFocusContext);
  const [widgetDimensions, setWidgetDimensions] = useState({});

  const widgets = useStore(WidgetStore, (state) => state.map(({ id, type }) => ({ id, type })).toArray().reverse());

  const positions = useWidgetPositions();

  const fields = useQueryFieldTypes();

  const children = useMemo(() => widgets.map(({ id: widgetId }) => {
    const position = positions[widgetId];

    if (!position) {
      return null;
    }

    const gridCoordinates = mapPosition(widgetId, position);
    const coordinatesIdentifier = JSON.stringify(gridCoordinates);

    return (
      <WidgetContainer key={`${widgetId}-${coordinatesIdentifier}`} data-grid={gridCoordinates} isFocused={focusedWidget?.id === widgetId && focusedWidget?.focusing}>
        <WidgetGridItem fields={fields}
                        positions={positions}
                        widgetId={widgetId}
                        setWidgetDimensions={setWidgetDimensions}
                        widgetDimensions={widgetDimensions}
                        focusedWidget={focusedWidget}
                        onPositionsChange={onPositionChange} />
      </WidgetContainer>
    );
  }).filter((x) => (x !== null)), [fields, focusedWidget, positions, widgetDimensions, widgets]);

  // The SizeMe component is required to update the widget grid
  // when its content height results in a scrollbar
  return (
    <DashboardWrap>
      <Grid locked={!isInteractive}
            onPositionsChange={onPositionsChange}>
        {children}
      </Grid>
    </DashboardWrap>
  );
};

WidgetGrid.displayName = 'WidgetGrid';

const MemoizedWidgetGrid = React.memo(WidgetGrid);

export default MemoizedWidgetGrid;
