import React from 'react';

const DataTableElement = React.createClass({
  propTypes: {
    element: React.PropTypes.any,
    formatter: React.PropTypes.func.isRequired,
    index: React.PropTypes.number,
  },
  render() {
    return this.props.formatter(this.props.element, this.props.index);
  },
});

export default DataTableElement;
