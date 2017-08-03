import PropTypes from 'prop-types';
import React from 'react';

const DataTableElement = React.createClass({
  propTypes: {
    element: PropTypes.any,
    formatter: PropTypes.func.isRequired,
    index: PropTypes.number,
  },
  render() {
    return this.props.formatter(this.props.element, this.props.index);
  },
});

export default DataTableElement;
