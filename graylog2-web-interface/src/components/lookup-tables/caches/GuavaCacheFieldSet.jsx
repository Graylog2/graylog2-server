import React, { PropTypes } from 'react';

const NullCacheFieldSet = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
    onChange: PropTypes.func.isRequired,
  },

  render() {
    return null;
  },
});

export default NullCacheFieldSet;
