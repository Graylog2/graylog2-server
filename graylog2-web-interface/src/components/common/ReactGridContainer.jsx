import React from 'react';
import ReactGridLayout, { WidthProvider } from 'react-grid-layout';
const WidthAdjustedReactGridLayout = WidthProvider(ReactGridLayout);

import 'react-grid-layout/css/styles.css';
import 'react-resizable/css/styles.css';
import style from './ReactGridContainer.css';

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

    return (
      <WidthAdjustedReactGridLayout className={style.reactGridLayout} layout={layout} cols={4} rowHeight={200}
                                    margin={[10, 10]}
                                    onDragStop={this._onLayoutChange}
                                    onResizeStop={this._onLayoutChange}
                                    isDraggable={!this.props.locked} isResizable={!this.props.locked}>
        {this.props.children}
      </WidthAdjustedReactGridLayout>
    );
  },
});

export default ReactGridContainer;
