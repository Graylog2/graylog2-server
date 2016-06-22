import React, { PropTypes } from 'react';

const LegacyLdapConfig = React.createClass({
  propTypes: {
    config: PropTypes.object,
  },
  render() {
    return (<span>Configure LDAP/AD integration here.</span>);
  },
});

export default LegacyLdapConfig;
