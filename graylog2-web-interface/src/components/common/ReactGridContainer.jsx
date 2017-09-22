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

const ReactGridContainer = React.createClass({
  propTypes: {
    positions: PropTypes.object.isRequired,
    children: PropTypes.node.isRequired,
    onPositionsChange: PropTypes.func.isRequired,
    locked: PropTypes.bool,
    isResizable: React.PropTypes.bool,
    rowHeight: React.PropTypes.number,
  },

  getDefaultProps() {
    return {
      locked: false,
      isResizable: true,
      rowHeight: ROW_HEIGHT,
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
    const { children, locked, isResizable, positions, rowHeight } = this.props;
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
                                    cols={COLUMNS}
                                    rowHeight={rowHeight}
                                    margin={[10, 10]}
                                    onDragStop={this._onLayoutChange}
                                    onResizeStop={this._onLayoutChange}
                                    draggableHandle={locked ? '.no-handle' : ''}>
        {children}
      </WidthAdjustedReactGridLayout>
    );
  },
});

export default ReactGridContainer;
