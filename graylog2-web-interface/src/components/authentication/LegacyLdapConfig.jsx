import React, { PropTypes } from 'react';
import { DocumentTitle, PageHeader } from 'components/common';
import { Button } from 'react-bootstrap';

import LdapComponent from 'components/ldap/LdapComponent';
import LdapGroupsComponent from 'components/ldap/LdapGroupsComponent';

import CombinedProvider from 'injection/CombinedProvider';
const { LdapActions } = CombinedProvider.get('Ldap');

import Routes from 'routing/Routes';

const LegacyLdapConfig = React.createClass({
  propTypes: {
    history: PropTypes.object.isRequired,
  },
  getInitialState() {
    return {
      showSettings: true,
    };
  },

  componentDidMount() {
    LdapActions.loadSettings();
  },

  _toggleButton() {
    this.setState({ showSettings: !this.state.showSettings });
  },

  _onSettingsCancel() {
    this._toggleButton();
  },

  _onCancel() {
    this.props.history.pushState(null, Routes.SYSTEM.AUTHENTICATION.OVERVIEW);
  },

  render() {
    const toggleButtonText = this.state.showSettings ? 'LDAP Group Mapping' : 'LDAP Settings';
    const activeComponent = (this.state.showSettings ?
      <LdapComponent onCancel={this._onCancel} onShowGroups={this._toggleButton} /> :
      <LdapGroupsComponent onCancel={this._onSettingsCancel} onShowConfig={this._toggleButton} />);

    return (
      <DocumentTitle title="LDAP Settings">
        <span>
          <PageHeader title="LDAP Settings" subpage>
            <span>
              This page is the only resource you need to set up the Graylog LDAP integration. You can test the
              connection to your LDAP server and even try to log in with an LDAP account of your choice right away.
            </span>
            {null}
            <span>
              <Button bsStyle="success" onClick={this._toggleButton}>{toggleButtonText}</Button>
            </span>
          </PageHeader>
          {activeComponent}
        </span>
      </DocumentTitle>
    );
  },
});

export default LegacyLdapConfig;
