import PropTypes from 'prop-types';
import React from 'react';

/**
 * Component used to encapsulate each header or row inside a `DataTable`. You probably
 * should not use this component directly, but through `DataTable`.
 */
const DataTableElement = React.createClass({
  propTypes: {
    /** Element to be formatted. */
    element: PropTypes.any,
    /**
     * Formatter function. It expects to receive the `element`, and `index` as arguments and
     * returns an element to be rendered.
     */
    formatter: PropTypes.func.isRequired,
    /** Element index. */
    index: PropTypes.number,
  },
  render() {
    return this.props.formatter(this.props.element, this.props.index);
  },
});

export default DataTableElement;
