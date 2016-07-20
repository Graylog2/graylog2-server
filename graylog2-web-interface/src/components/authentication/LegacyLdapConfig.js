import React, { PropTypes } from 'react';
import { PageHeader } from 'components/common';
import { Button } from 'react-bootstrap';

import LdapComponent from 'components/ldap/LdapComponent';
import LdapGroupsComponent from 'components/ldap/LdapGroupsComponent';

const LegacyLdapConfig = React.createClass({
  propTypes: {
    config: PropTypes.object,
  },
  getInitialState() {
    return {
      showSettings: true,
    };
  },

  _toggleButton() {
    this.setState({ showSettings: !this.state.showSettings });
  },
  render() {
    const toggleButtonText = this.state.showSettings ? 'Ldap Group Mapping' : 'Ldap Settings';
    const activeComponent = this.state.showSettings ? <LdapComponent /> : <LdapGroupsComponent />

    return (<span>
      <PageHeader title="LDAP Settings" subpage>
        <span>This page is the only resource you need to set up the Graylog LDAP integration. You can test the connection to your LDAP server and even try to log in with an LDAP account of your choice right away.</span>
        {null}
        <span>
          <Button bsStyle="info" onClick={this._toggleButton}>{toggleButtonText}</Button>
        </span>
      </PageHeader>
      {activeComponent}
    </span>);
  },
});

export default LegacyLdapConfig;
