import React, { PropTypes } from 'react';

const NullCacheSummary = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
  },

  render() {
    return (<div>
      <h2>No cache.</h2>
      <p>This lookup table is uncached.</p>
    </div>);
  },
});

export default NullCacheSummary;
