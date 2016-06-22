import React, { PropTypes } from 'react';

const MongoDbPasswordConfig = React.createClass({
  propTypes: {
    config: PropTypes.object,
  },
  render() {
    return (<span>Configure password authentication from MongoDB here.</span>);
  },
});

export default MongoDbPasswordConfig;
