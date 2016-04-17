import React from 'react';

const DataTableElement = React.createClass({
  propTypes: {
    element: React.PropTypes.any,
    formatter: React.PropTypes.func.isRequired,
  },
  render() {
    return this.props.formatter(this.props.element);
  },
});

export default DataTableElement;