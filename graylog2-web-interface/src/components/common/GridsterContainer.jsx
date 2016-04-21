import React, { PropTypes } from 'react';
import ReactDOM from 'react-dom';

import { GridsterWidget } from 'components/common';

require('!script!../../../public/javascripts/jquery-2.1.1.min.js');
require('!script!../../../public/javascripts/jquery.gridster.min.js');

const GridsterContainer = React.createClass({
  propTypes: {
    positions: PropTypes.object.isRequired,
    children: PropTypes.node.isRequired,
    onPositionsChange: PropTypes.func.isRequired,
  },

  getInitialState() {
    return { grid: undefined };
  },

  componentDidMount() {
    this._initializeDashboard();
  },

  WIDGET_WIDTH: 410,
  WIDGET_HEIGHT: 200,

  _initializeDashboard() {
    const rootNode = ReactDOM.findDOMNode(this.refs.gridster);
    const grid = this._initGridster(rootNode);

    this._lockGrid(grid);

    this.setState({ grid: grid });
  },

  _initGridster(rootNode) {
    const minColumns = Object.keys(this.props.positions).reduce((max, current) => {
      const position = this.props.positions[current];
      // Column may be set to 0 as default, but the minimum effective column is 1
      const effectiveColumn = Math.max(position.col, 1);
      return Math.max(max, effectiveColumn + position.width - 1);
    }, 0);

    return $(rootNode).gridster({
      widget_margins: [10, 10],
      widget_base_dimensions: [this.WIDGET_WIDTH, this.WIDGET_HEIGHT],
      autogrow_cols: true,
      min_cols: minColumns,
      resize: {
        enabled: true,
        stop: this._onPositionsChange,
      },
      draggable: {
        stop: this._onPositionsChange,
      },
      serialize_params: (widgetListItem, position) => {
        const widget = $('.widget', widgetListItem);

        return {
          id: widget.attr('data-widget-id'),
          col: position.col,
          row: position.row,
          size_x: position.size_x,
          size_y: position.size_y,
        };
      },
    }).data('gridster');
  },
  lockGrid() {
    this._lockGrid(this.state.grid);
  },
  unlockGrid() {
    this._unlockGrid(this.state.grid);
  },
  _lockGrid(grid) {
    grid.disable();
    grid.disable_resize();
  },
  _unlockGrid(grid) {
    grid.enable();
    grid.enable_resize();
  },
  _onPositionsChange() {
    const positions = this.state.grid.serialize().map((position) => {
      return { id: position.id, col: position.col, row: position.row, width: position.size_x, height: position.size_y };
    });
    this.props.onPositionsChange(positions);
  },

  render() {
    const children = (this.state.grid && React.Children.map(this.props.children, (child) => {
      const position = this.props.positions[child.props.id] || {row: 0, col: 0, width: 1, height: 1};

      return (
        <GridsterWidget grid={this.state.grid} position={position}>
          {child}
        </GridsterWidget>
      );
    }));
    return (
      <ul ref="gridster" className="gridster">
        {children}
      </ul>
    );
  },
});

export default GridsterContainer;
