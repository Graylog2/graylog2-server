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
import { useContext, useMemo } from 'react';
import styled, { css } from 'styled-components';

import type { WidgetPositions, BackendWidgetPosition } from 'views/types';
import ReactGridContainer from 'components/common/ReactGridContainer';
import { widgetDefinition } from 'views/logic/Widgets';
import WidgetPosition from 'views/logic/widgets/WidgetPosition';
import type { FocusContextState } from 'views/components/contexts/WidgetFocusContext';
import WidgetFocusContext from 'views/components/contexts/WidgetFocusContext';
import type { FieldTypeMappingsList } from 'views/logic/fieldtypes/types';
import { useStore } from 'stores/connect';
import { WidgetStore } from 'views/stores/WidgetStore';
import { CurrentViewStateActions } from 'views/stores/CurrentViewStateStore';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import InteractiveContext from 'views/components/contexts/InteractiveContext';
import type { StoreState } from 'stores/StoreTypes';
import { ViewStatesStore } from 'views/stores/ViewStatesStore';
import ElementDimensions from 'components/common/ElementDimensions';
import useActiveQueryId from 'views/hooks/useActiveQueryId';
import findGaps from 'views/components/GridGaps';
import generateId from 'logic/generateId';
import NewWidgetPlaceholder from 'views/components/NewWidgetPlaceholder';
import CreateNewWidgetModal from 'views/components/CreateNewWidgetModal';

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

const DashboardWrap = styled(ElementDimensions)(({ theme }) => css`
  color: ${theme.colors.global.textDefault};
  margin: 0;
  width: 100%;
  height: 100%;
`);

const StyledReactGridContainer = styled(ReactGridContainer)(({ $hasFocusedWidget }: { $hasFocusedWidget: boolean }) => css`
  height: ${$hasFocusedWidget ? '100% !important' : '100%'};
  max-height: ${$hasFocusedWidget ? '100%' : 'auto'};
  overflow: ${$hasFocusedWidget ? 'hidden' : 'visible'};
  transition: none;
`);

const _defaultDimensions = (type: string) => {
  const widgetDef = widgetDefinition(type);

  return new WidgetPosition(1, 1, widgetDef.defaultHeight, widgetDef.defaultWidth);
};

type WidgetsProps = {
  fields: FieldTypeMappingsList,
  widgetId: string,
  focusedWidget: FocusContextState | undefined,
  onPositionsChange: (position: BackendWidgetPosition) => void,
  positions: WidgetPositions,
};

const WidgetGridItem = ({
  fields,
  onPositionsChange,
  positions,
  widgetId,
  focusedWidget,
}: WidgetsProps) => {
  const editing = focusedWidget?.id === widgetId && focusedWidget?.editing;
  const widgetPosition = positions[widgetId];

  return (
    <WidgetComponent editing={editing}
                     fields={fields}
                     onPositionsChange={onPositionsChange}
                     position={widgetPosition}
                     widgetId={widgetId} />
  );
};

const generatePositions = (widgets: Array<{ id: string, type: string }>, positions: { [widgetId: string]: WidgetPosition }) => Object.fromEntries(
  widgets.map<[string, WidgetPosition]>(({ id, type }) => [id, positions[id] ?? _defaultDimensions(type)]),
);

const mapWidgetPositions = (states: StoreState<typeof ViewStatesStore>) => Object.fromEntries(states.toArray().flatMap((state) => Object.entries(state.widgetPositions)));
const mapWidgets = (state: StoreState<typeof WidgetStore>) => state.map(({ id, type }) => ({ id, type })).toArray();

const useWidgetPositions = (): WidgetPositions => {
  const initialPositions = useStore(ViewStatesStore, mapWidgetPositions);
  const widgets = useStore(WidgetStore, mapWidgets);

  return useMemo(() => generatePositions(widgets, initialPositions), [widgets, initialPositions]);
};

type GridProps = {
  children: React.ReactNode,
  locked: boolean,
  onPositionsChange: (newPositions: Array<BackendWidgetPosition>) => void,
  positions: WidgetPositions,
  width: number,
};

const Grid = ({ children, locked, onPositionsChange, positions, width }: GridProps) => {
  const { focusedWidget } = useContext(WidgetFocusContext);

  return (
    <StyledReactGridContainer $hasFocusedWidget={!!focusedWidget?.id}
                              columns={COLUMNS}
                              isResizable={!focusedWidget?.id}
                              locked={locked}
                              positions={positions}
                              measureBeforeMount
                              onPositionsChange={onPositionsChange}
                              width={width}
                              draggableHandle=".widget-drag-handle">
      {children}
    </StyledReactGridContainer>
  );
};

const useQueryFieldTypes = () => {
  const fieldTypes = useContext(FieldTypesContext);
  const queryId = useActiveQueryId();

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

const renderGaps = (widgets: { id: string, type: string}[], positions: WidgetPositions) => {
  const items = widgets.map((widget) => positions[widget.id])
    .filter((position) => !!position)
    .map((p) => ({ start: { x: p.col, y: p.row }, end: { x: p.col + p.width, y: p.row + p.height } }));
  const gaps = findGaps(items);
  const _positions = { ...positions };

  const gapsItems = gaps.map((gap) => {
    const id = `gap-${generateId()}`;

    const gapPosition = WidgetPosition.builder()
      .col(gap.start.x)
      .row(gap.start.y)
      .height(gap.end.y - gap.start.y)
      .width(gap.end.x - gap.start.x)
      .build();

    _positions[id] = gapPosition;

    return (
      <NewWidgetPlaceholder key={id} position={gapPosition} component={CreateNewWidgetModal} />
    );
  });

  return [gapsItems, _positions] as const;
};

const WidgetGrid = () => {
  const isInteractive = useContext(InteractiveContext);
  const { focusedWidget } = useContext(WidgetFocusContext);

  const widgets = useStore(WidgetStore, (state) => state.map(({ id, type }) => ({ id, type })).toArray().reverse());

  const positions = useWidgetPositions();

  const fields = useQueryFieldTypes();

  const [children, newPositions] = useMemo(() => {
    const widgetItems = widgets
      .filter((widget) => !!positions[widget.id])
      .map(({ id: widgetId }) => (
        <WidgetContainer key={widgetId} isFocused={focusedWidget?.id === widgetId && focusedWidget?.focusing}>
          <WidgetGridItem fields={fields}
                          positions={positions}
                          widgetId={widgetId}
                          focusedWidget={focusedWidget}
                          onPositionsChange={onPositionChange} />
        </WidgetContainer>
      ));

    if (isInteractive) {
      const [gapItems, _positions] = renderGaps(widgets, positions);

      return [[...widgetItems, ...gapItems], _positions];
    }

    return [widgetItems, positions];
  }, [fields, focusedWidget, isInteractive, positions, widgets]);

  // Measuring the width is required to update the widget grid
  // when its content height results in a scrollbar
  return (
    <DashboardWrap>
      {({ width }) => (
        <Grid locked={!isInteractive}
              positions={newPositions}
              onPositionsChange={onPositionsChange}
              width={width}>
          {children}
        </Grid>
      )}
    </DashboardWrap>
  );
};

WidgetGrid.displayName = 'WidgetGrid';

const MemoizedWidgetGrid = React.memo(WidgetGrid);

export default MemoizedWidgetGrid;
