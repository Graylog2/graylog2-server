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
import { useCallback, useMemo } from 'react';
import styled, { css, useTheme } from 'styled-components';
import { Responsive, WidthProvider } from 'react-grid-layout';
import type { ItemCallback } from 'react-grid-layout';

import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';
import type { WidgetPositionJSON } from 'views/logic/widgets/WidgetPosition';
import type WidgetPosition from 'views/logic/widgets/WidgetPosition';
import { layoutToPositions, positionsToLayout } from 'views/logic/widgets/normalizeWidgetPositions';

const WidthAdjustedReactGridLayout = WidthProvider(Responsive);

const WidthProvidedGridLayout = ({ width, ...props }: React.ComponentProps<typeof WidthAdjustedReactGridLayout> & { children: React.ReactNode }) => (width
  ? <Responsive width={width} {...props} />
  : <WidthAdjustedReactGridLayout width={width} {...props} />);

const StyledWidthProvidedGridLayout = styled(WidthProvidedGridLayout)(({ theme }) => css`
  &.locked {
    .widget-drag-handle {
      display: none;
    }
  }

  &.unlocked {
    .react-draggable {
      cursor: move;
    }
  }

  .react-grid-item.react-grid-placeholder {
    background: ${theme.colors.variant.info};
  }

  .actions {
    cursor: default;
  }
`);

const COLUMN_WIDTH = 175;
const ROW_HEIGHT = 100;

const COLUMNS = {
  xxl: 12,
  xl: 10,
  lg: 8,
  md: 6,
  sm: 4,
  xs: 2,
};

const BREAKPOINTS = {
  xxl: COLUMN_WIDTH * COLUMNS.xxl,
  xl: COLUMN_WIDTH * COLUMNS.xl,
  lg: COLUMN_WIDTH * COLUMNS.lg,
  md: COLUMN_WIDTH * COLUMNS.md,
  sm: COLUMN_WIDTH * COLUMNS.sm,
  xs: COLUMN_WIDTH * COLUMNS.xs,
};

const _gridClass = (locked: boolean, isResizable: boolean, draggableHandle: string, propsClassName: string) => {
  const className = `${propsClassName}`;

  if (locked || !isResizable) {
    return `${className} locked`;
  }

  if (draggableHandle) {
    return className;
  }

  return `${className} unlocked`;
};

export type Position = {
  id: string,
  col: number,
  row: number,
  height: number,
  width: number
};

const _onLayoutChange = (newLayout: Layout, callback: ((newPositions: Position[]) => void) | undefined) => {
  if (typeof callback !== 'function') {
    return undefined;
  }

  const newPositions: Position[] = layoutToPositions(newLayout.filter(({ i }) => !i.startsWith('gap')));

  return callback(newPositions);
};

type Props = {
  children: React.ReactNode,
  className?: string,
  columns?: {
    xxl: number,
    xl: number,
    lg: number,
    md: number,
    sm: number,
    xs: number,
  },
  draggableHandle?: string,
  isResizable?: boolean,
  locked?: boolean,
  measureBeforeMount?: boolean,
  onPositionsChange: (newPositions: Array<WidgetPositionJSON>) => void,
  onSyncLayout?: (newPositions: Array<WidgetPositionJSON>) => void,
  positions: { [widgetId: string]: WidgetPosition },
  rowHeight?: number,
  width?: number,
}

export type LayoutItem = { i: string, x: number, y: number, h: number, w: number };
export type Layout = Array<LayoutItem>;

const removeGaps = (_layout: Layout) => {
  const gapIndices = [];

  _layout.forEach((item, idx) => {
    if (item.i.startsWith('gap')) {
      gapIndices.push(idx);
    }
  });

  gapIndices.reverse().forEach((idx) => _layout.splice(idx, 1));
};

/**
 * Component that renders a draggable and resizable grid. You can control
 * the grid elements' positioning, as well as if they should be resizable
 * or draggable. Use this for dashboards or pages where the user should
 * be able to decide how to arrange the content.
 */
const ReactGridContainer = ({
  children,
  className,
  columns = COLUMNS,
  draggableHandle,
  isResizable = true,
  locked = false,
  measureBeforeMount = false,
  onPositionsChange,
  onSyncLayout: _onSyncLayout,
  positions,
  rowHeight = ROW_HEIGHT,
  width,
}: Props) => {
  const theme = useTheme();
  const cellMargin = theme.spacings.px.xs;
  const onLayoutChange = useCallback<ItemCallback>((layout) => _onLayoutChange(layout, onPositionsChange), [onPositionsChange]);
  const onSyncLayout = useCallback((layout: Layout) => _onLayoutChange(layout, _onSyncLayout), [_onSyncLayout]);
  const gridClass = _gridClass(locked, isResizable, draggableHandle, className);
  const layout = useMemo(() => positionsToLayout(positions), [positions]);

  // We need to use a className and draggableHandle to avoid re-rendering all graphs on lock/unlock. See:
  // https://github.com/STRML/react-grid-layout/issues/371
  return (
    <StyledWidthProvidedGridLayout className={gridClass}
                                   width={width}
                                   breakpoints={BREAKPOINTS}
                                   cols={columns}
                                   layouts={{ xxl: layout, xl: layout, lg: layout, md: layout, sm: layout, xs: layout }}
                                   rowHeight={rowHeight}
                                   containerPadding={[0, 0]}
                                   margin={[cellMargin, cellMargin]}
                                   isResizable={!locked && isResizable}
                                   isDraggable={!locked}
                                   measureBeforeMount={measureBeforeMount}
      // Do not allow dragging from elements inside a `.actions` css class. This is
      // meant to avoid calling `onDragStop` callbacks when clicking on an action button.
                                   draggableCancel=".actions"
                                   onDragStart={removeGaps}
                                   onDragStop={onLayoutChange}
                                   onResizeStart={removeGaps}
                                   onResizeStop={onLayoutChange}
                                   onLayoutChange={onSyncLayout}
      // While CSS transform improves the paint performance,
      // it currently results in bug when using `react-sticky-el` inside a grid item.
                                   useCSSTransforms={false}
                                   draggableHandle={locked ? '' : draggableHandle}>
      {children}
    </StyledWidthProvidedGridLayout>
  );
};

export default ReactGridContainer;
