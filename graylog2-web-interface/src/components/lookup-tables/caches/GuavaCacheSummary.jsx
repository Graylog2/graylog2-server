import React, { PropTypes } from 'react';

const GuavaCacheSummary = React.createClass({
  propTypes: {
    config: PropTypes.object.isRequired,
  },

  render() {
    return (<div>
      <h2>Node-local, in-memory cache.</h2>
      <p>TODO: describe configuration.</p>
    </div>);
  },
});

export default GuavaCacheSummary;
