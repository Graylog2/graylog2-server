import PropTypes from 'prop-types';
import React from 'react';
import { Responsive, WidthProvider } from 'react-grid-layout';

import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';
import style from './ReactGridContainer.css';

const WidthAdjustedReactGridLayout = WidthProvider(Responsive);

const COLUMN_WIDTH = 350;
const ROW_HEIGHT = 200;

const COLUMNS = {
  xxl: 6,
  xl: 5,
  lg: 4,
  md: 3,
  sm: 2,
  xs: 1,
};

const BREAKPOINTS = {
  xxl: COLUMN_WIDTH * COLUMNS.xxl,
  xl: COLUMN_WIDTH * COLUMNS.xl,
  lg: COLUMN_WIDTH * COLUMNS.lg,
  md: COLUMN_WIDTH * COLUMNS.md,
  sm: COLUMN_WIDTH * COLUMNS.sm,
  xs: COLUMN_WIDTH * COLUMNS.xs,
};

/**
 * Component that renders a draggable and resizable grid. You can control
 * the grid elements' positioning, as well as if they should be resizable
 * or draggable. Use this for dashboards or pages where the user should
 * be able to decide how to arrange the content.
 */
const ReactGridContainer = React.createClass({
  propTypes: {
    /**
     * Object of positions in this format:
     * ```
     * {
     *  id: { col: column, row: row, height: height, width: width },
     *  // E.g.
     *  '2': { col: 2, row: 0, height: 1, width: 4 },
     * }
     * ```
     *
     * **Important** All positions and sizes are specified in grid coordinates,
     * not in pixels.
     */
    positions: PropTypes.object.isRequired,
    /**
     * Array of children, each one being one element in the grid. Each
     * children's outermost element must have a `key` prop set to the `id`
     * specified in the position object. If you don't set that `key` to the
     * right value, the positioning will be wrong.
     */
    children: PropTypes.node.isRequired,
    /**
     * Function that will be called when positions change. The function
     * receives the new positions in the same format as specified in the
     * `positions` prop.
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
  },

  getDefaultProps() {
    return {
      locked: false,
      isResizable: true,
      rowHeight: ROW_HEIGHT,
      columns: COLUMNS,
      animate: true,
    };
  },

  _onLayoutChange(newLayout) {
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

    this.props.onPositionsChange(newPositions);
  },

  render() {
    const { children, locked, isResizable, positions, rowHeight, columns, animate } = this.props;
    const layout = Object.keys(positions).map((id) => {
      const { col, row, height, width } = positions[id];
      return {
        i: id,
        x: col ? Math.max(col - 1, 0) : 0,
        y: (row === undefined || row <= 0 ? Infinity : row - 1),
        h: height || 1,
        w: width || 1,
      };
    });

    // We need to use a className and draggableHandle to avoid re-rendering all graphs on lock/unlock. See:
    // https://github.com/STRML/react-grid-layout/issues/371
    return (
      <WidthAdjustedReactGridLayout className={`${style.reactGridLayout} ${locked || !isResizable ? 'locked' : 'unlocked'}`}
                                    layouts={{ xxl: layout, xl: layout, lg: layout, md: layout, sm: layout, xs: layout }}
                                    breakpoints={BREAKPOINTS}
                                    cols={columns}
                                    rowHeight={rowHeight}
                                    margin={[10, 10]}
                                    onDragStop={this._onLayoutChange}
                                    onResizeStop={this._onLayoutChange}
                                    useCSSTransforms={animate}
                                    draggableHandle={locked ? '.no-handle' : ''}>
        {children}
      </WidthAdjustedReactGridLayout>
    );
  },
});

export default ReactGridContainer;
