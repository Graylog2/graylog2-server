import React, {PropTypes} from 'react';

const Pluralize = React.createClass({
  propTypes: {
    singular: PropTypes.string.isRequired,
    plural: PropTypes.string.isRequired,
    value: PropTypes.oneOfType([
      PropTypes.number,
      PropTypes.string,
    ]).isRequired,
  },
  render() {
    return <span>{this.props.value === 1 || this.props.value === '1' ? this.props.singular : this.props.plural}</span>;
  },
});

export default Pluralize;
