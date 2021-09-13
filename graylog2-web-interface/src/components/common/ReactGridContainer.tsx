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
import PropTypes from 'prop-types';
import React from 'react';
import { Responsive, WidthProvider } from 'react-grid-layout';
import styled, { css, withTheme } from 'styled-components';

import { themePropTypes } from 'theme';

import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';
import { WidgetPositionJSON } from 'views/logic/widgets/WidgetPosition';

const WidthAdjustedReactGridLayout = WidthProvider(Responsive);

const WidthProvidedGridLayout = (props) => {
  const { width } = props;

  return width ? <Responsive {...props} /> : <WidthAdjustedReactGridLayout {...props} />;
};

WidthProvidedGridLayout.propTypes = { width: PropTypes.number };
WidthProvidedGridLayout.defaultProps = { width: undefined };

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

const _gridClass = (locked, isResizable, draggableHandle, propsClassName) => {
  const className = `${propsClassName}`;

  if (locked || !isResizable) {
    return `${className} locked`;
  }

  if (draggableHandle) {
    return className;
  }

  return `${className} unlocked`;
};

type Props = {
  animate?: boolean,
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
  rowHeight?: number,
  theme,
  width?: number,
}

/**
 * Component that renders a draggable and resizable grid. You can control
 * the grid elements' positioning, as well as if they should be resizable
 * or draggable. Use this for dashboards or pages where the user should
 * be able to decide how to arrange the content.
 */
class ReactGridContainer extends React.Component<Props> {
  static propTypes = {
    /**
     * Array of children, each one being one element in the grid. Each
     * children's outermost element must have a `key` prop set to the `id`
     * specified in the position object. If you don't set that `key` to the
     * right value, the positioning will be wrong.
     */
    children: PropTypes.node.isRequired,
    /**
     * The className prop is necessary to style the component with styled-components `styled()` function.
     */
    className: PropTypes.string,
    /**
     * Function that will be called when positions change. The function
     * receives the new positions in the format:
     *
     * ```
     * [
     *   { id: widgetId, col: column, row: row, height: height, width: width },
     *   // E.g.
     *   { id: '2', col: 2, row: 0, height: 1, width: 4 },
     * ]
     * ```
     */
    onPositionsChange: PropTypes.func.isRequired,
    /**
     * Specifies if the grid is locked or not. A user cannot move or
     * resize grid elements if this is set to true.
     */
    locked: PropTypes.bool,
    /**
     * Specifies if the grid elements can be resized or not **only when the
     * grid is unlocked**.
     */
    isResizable: PropTypes.bool,
    /** Height in pixels of a row. */
    rowHeight: PropTypes.number,
    /**
     * Specifies the number of columns the grid will have for different
     * screen sizes. E.g.
     * ```
     * {
     *   xxl: 6,
     *   xl: 5,
     *   lg: 4,
     *   md: 3,
     *   sm: 2,
     *   xs: 1,
     * }
     * ```
     *
     * Each column is by default 350 pixels wide.
     */
    columns: PropTypes.object,
    /** Specifies whether the grid should use CSS animations or not. */
    animate: PropTypes.bool,
    /**
     * Specifies whether (and which css class) a drag handle should be used.
     *
     * If this prop is not specified, the whole widget can be used for dragging when the grid is unlocked.
     *
     * If this prop is defined, the css class specified will define which item can be used for dragging if unlocked.
     *
     */
    draggableHandle: PropTypes.string,
    /**
     * Specifies whether the grid is measured before mounting the grid component. Otherwise the grid is initialized with
     * a width of 1280 before it is being resized.
     *
     * See: https://github.com/STRML/react-grid-layout/blob/0.14.3/lib/components/WidthProvider.jsx#L20-L21
     *
     */
    measureBeforeMount: PropTypes.bool,
    width: PropTypes.number,
    theme: themePropTypes.isRequired,
  };

  static defaultProps = {
    animate: false,
    className: undefined,
    columns: COLUMNS,
    isResizable: true,
    locked: false,
    measureBeforeMount: false,
    rowHeight: ROW_HEIGHT,
    draggableHandle: undefined,
    width: undefined,
  };

  _onLayoutChange = (newLayout) => {
    const newPositions = [];

    newLayout.forEach((widget) => {
      newPositions.push({
        id: widget.i,
        col: widget.x + 1,
        row: widget.y + 1,
        height: widget.h,
        width: widget.w,
      });
    });

    const { onPositionsChange } = this.props;

    onPositionsChange(newPositions);
  };

  render() {
    const {
      animate,
      children,
      className,
      columns,
      isResizable,
      locked,
      measureBeforeMount,
      rowHeight,
      theme,
      draggableHandle,
      width,
    } = this.props;
    // const { layout } = this.state;
    const cellMargin = theme.spacings.px.xs;

    // We need to use a className and draggableHandle to avoid re-rendering all graphs on lock/unlock. See:
    // https://github.com/STRML/react-grid-layout/issues/371
    return (
      <StyledWidthProvidedGridLayout className={_gridClass(locked, isResizable, draggableHandle, className)}
                                     width={width}
                                     breakpoints={BREAKPOINTS}
                                     cols={columns}
                                     rowHeight={rowHeight}
                                     containerPadding={[0, 0]}
                                     margin={[cellMargin, cellMargin]}
                                     isResizable={!locked && isResizable}
                                     isDraggable={!locked}
                                     measureBeforeMount={measureBeforeMount}
        // Do not allow dragging from elements inside a `.actions` css class. This is
        // meant to avoid calling `onDragStop` callbacks when clicking on an action button.
                                     draggableCancel=".actions"
                                     onDragStop={this._onLayoutChange}
                                     onResizeStop={this._onLayoutChange}
                                     useCSSTransforms={animate}
                                     draggableHandle={locked ? '' : draggableHandle}>
        {children}
      </StyledWidthProvidedGridLayout>
    );
  }
}

export default withTheme(ReactGridContainer);
