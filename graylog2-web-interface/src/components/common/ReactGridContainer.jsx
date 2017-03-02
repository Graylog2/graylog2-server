import React from 'react';
import { Responsive, WidthProvider } from 'react-grid-layout';
const WidthAdjustedReactGridLayout = WidthProvider(Responsive);

import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';
import style from './ReactGridContainer.css';

const COLUMN_WIDTH = 350;

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
    positions: React.PropTypes.object.isRequired,
    children: React.PropTypes.node.isRequired,
    onPositionsChange: React.PropTypes.func.isRequired,
    locked: React.PropTypes.bool,
  },

  getDefaultProps() {
    return {
      locked: false,
    };
  },

  _onLayoutChange(newLayout) {
    const newPositions = [];
    newLayout.forEach(widget => {
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
    const layout = Object.keys(this.props.positions).map(id => {
      const position = this.props.positions[id];
      return {
        i: id,
        x: Math.max(position.col - 1, 0),
        y: (position.row <= 0 ? Infinity : position.row - 1),
        h: position.height,
        w: position.width,
      };
    });

    // We need to use a className and draggableHandle to avoid re-rendering all graphs on lock/unlock. See:
    // https://github.com/STRML/react-grid-layout/issues/371
    return (
      <WidthAdjustedReactGridLayout className={`${style.reactGridLayout} ${this.props.locked ? 'locked' : 'unlocked'}`}
                                    layouts={{ xxl: layout, xl: layout, lg: layout, md: layout, sm: layout, xs: layout }}
                                    breakpoints={BREAKPOINTS}
                                    cols={COLUMNS}
                                    rowHeight={200}
                                    margin={[10, 10]}
                                    onDragStop={this._onLayoutChange}
                                    onResizeStop={this._onLayoutChange}
                                    draggableHandle={this.props.locked ? '.no-handle' : ''}>
        {this.props.children}
      </WidthAdjustedReactGridLayout>
    );
  },
});

export default ReactGridContainer;
