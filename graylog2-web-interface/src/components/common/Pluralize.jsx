import React, { PropTypes } from 'react';

import StringUtils from 'util/StringUtils';

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
    return <span>{StringUtils.pluralize(this.props.value, this.props.singular, this.props.plural)}</span>;
  },
});

export default Pluralize;
