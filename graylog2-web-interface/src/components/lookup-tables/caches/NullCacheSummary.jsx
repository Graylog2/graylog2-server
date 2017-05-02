import React, { PropTypes } from 'react';

const NullCacheSummary = React.createClass({
  propTypes: {
    cache: PropTypes.object.isRequired,
  },

  render() {
    return (<p>This cache has no configuration.</p>);
  },
});

export default NullCacheSummary;
