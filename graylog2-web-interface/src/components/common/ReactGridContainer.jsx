import PropTypes from 'prop-types';
import React from 'react';
import { Responsive, WidthProvider } from 'react-grid-layout';
import lodash from 'lodash';

import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';
import style from './ReactGridContainer.css';

const WidthAdjustedReactGridLayout = WidthProvider(Responsive);

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

const _gridClass = (locked, isResizable, useDragHandle) => {
  if (locked || !isResizable) {
    return 'locked';
  }
  if (useDragHandle) {
    return '';
  }
  return 'unlocked';
};

/**
 * Component that renders a draggable and resizable grid. You can control
 * the grid elements' positioning, as well as if they should be resizable
 * or draggable. Use this for dashboards or pages where the user should
 * be able to decide how to arrange the content.
 */
class ReactGridContainer extends React.Component {
  static propTypes = {
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
    useDragHandle: PropTypes.string,
    /**
     * Specifies whether the grid is measured before mounting the grid component. Otherwise the grid is initialized with
     * a width of 1280 before it is being resized.
     *
     * See: https://github.com/STRML/react-grid-layout/blob/0.14.3/lib/components/WidthProvider.jsx#L20-L21
     *
     */
    measureBeforeMount: PropTypes.bool,
  };

  static defaultProps = {
    locked: false,
    isResizable: true,
    rowHeight: ROW_HEIGHT,
    columns: COLUMNS,
    animate: true,
    useDragHandle: undefined,
    measureBeforeMount: false,
  };

  componentWillReceiveProps(nextProps) {
    if (!lodash.isEqual(nextProps.positions, this.props.positions)) {
      this.setState({ layout: this.computeLayout(nextProps.positions) });
    }
  }

  computeLayout = (positions) => {
    return Object.keys(positions).map((id) => {
      const { col, row, height, width } = positions[id];
      return {
        i: id,
        x: col ? Math.max(col - 1, 0) : 0,
        y: (row === undefined || row <= 0 ? Infinity : row - 1),
        h: height || 1,
        w: width || 1,
      };
    });
  };

  _onLayoutChange = (newLayout) => {
    // `onLayoutChange` may be triggered when clicking somewhere in a widget, check before propagating the change.
    // Filter out additional Object properties in nextLayout, as it comes directly from react-grid-layout
    const filteredNewLayout = newLayout.map(item => ({ i: item.i, x: item.x, y: item.y, h: item.h, w: item.w }));
    if (lodash.isEqual(this.state.layout, filteredNewLayout)) {
      return;
    }

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
  };

  state = {
    layout: this.computeLayout(this.props.positions),
  };

  render() {
    const { children, locked, isResizable, rowHeight, columns, animate, useDragHandle, measureBeforeMount } = this.props;
    const { layout } = this.state;

    // We need to use a className and draggableHandle to avoid re-rendering all graphs on lock/unlock. See:
    // https://github.com/STRML/react-grid-layout/issues/371
    return (
      <WidthAdjustedReactGridLayout className={`${style.reactGridLayout} ${_gridClass(locked, isResizable, useDragHandle)}`}
                                    layouts={{ xxl: layout, xl: layout, lg: layout, md: layout, sm: layout, xs: layout }}
                                    breakpoints={BREAKPOINTS}
                                    cols={columns}
                                    rowHeight={rowHeight}
                                    margin={[10, 10]}
                                    measureBeforeMount={measureBeforeMount}
                                    // Do not allow dragging from elements inside a `.actions` css class. This is
                                    // meant to avoid calling `onDragStop` callbacks when clicking on an action button.
                                    draggableCancel=".actions"
                                    onDragStop={this._onLayoutChange}
                                    onResizeStop={this._onLayoutChange}
                                    useCSSTransforms={animate}
                                    draggableHandle={locked ? '.no-handle' : useDragHandle}>
        {children}
      </WidthAdjustedReactGridLayout>
    );
  }
}

export default ReactGridContainer;
