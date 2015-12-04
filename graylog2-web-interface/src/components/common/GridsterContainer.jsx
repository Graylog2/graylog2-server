import React from 'react';
import ReactDOM from 'react-dom';

import { GridsterWidget } from 'components/common';

require('!script!../../../public/javascripts/jquery-2.1.1.min.js');
require('!script!../../../public/javascripts/jquery.gridster.min.js');

const GridsterContainer = React.createClass({
  propTypes: {
    positions: React.PropTypes.object.isRequired,
    children: React.PropTypes.node.isRequired,
  },
  getInitialState() {
    return { grid: undefined };
  },
  componentDidMount() {
    const rootNode = ReactDOM.findDOMNode(this.refs.gridster);
    const grid = this._initGridster(rootNode);

    this._lockGrid(grid);

    this.setState({grid: grid});
  },
  _initGridster(rootNode) {
    return $(rootNode).gridster({
      widget_margins: [10, 10],
      widget_base_dimensions: [410, 200],
      resize: {
        enabled: true,
        stop: this._onPositionsChanged,
      },
      draggable: {
        stop: this._onPositionsChanged,
      },
      serialize_params: function(widgetListItem, pos) {
        const widget = $('.widget', widgetListItem);

        return {
          id: widget.attr('data-widget-id'),
          col: pos.col,
          row: pos.row,
          size_x: pos.size_x,
          size_y: pos.size_y,
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
