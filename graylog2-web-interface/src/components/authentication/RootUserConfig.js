import React, { PropTypes } from 'react';

const RootUserConfig = React.createClass({
  propTypes: {
    config: PropTypes.object,
  },
  render() {
    return (<span>Configure the hardcoded root user authentication here (probably nothing to configure for now).</span>);
  },
});

export default RootUserConfig;
