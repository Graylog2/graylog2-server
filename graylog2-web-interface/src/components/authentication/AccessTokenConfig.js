import React, { PropTypes } from 'react';

const AccessTokenConfig = React.createClass({
  propTypes: {
    config: PropTypes.object,
  },
  render() {
    return (<span>Configure general access token handling here.</span>);
  },
});

export default AccessTokenConfig;
