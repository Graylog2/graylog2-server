import React, { PropTypes } from 'react';

const MongoDbSessionConfig = React.createClass({
  propTypes: {
    config: PropTypes.object,
  },
  render() {
    return (<span>Configure session handling (stored in MongoDB) here.</span>);
  },
});

export default MongoDbSessionConfig;
