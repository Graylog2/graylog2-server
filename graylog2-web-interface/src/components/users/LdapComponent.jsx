import React from 'react';
import Immutable from 'immutable';

import { Row, Col, Input, Panel, Button } from 'react-bootstrap';

import Spinner from '../common/Spinner';

import RolesStore from 'stores/users/RolesStore';
import LdapStore from 'stores/users/LdapStore';
import LdapGroupsStore from 'stores/users/LdapGroupsStore';

const LdapComponent = React.createClass({
  getInitialState() {
    return {
      ldapSettings: null,
    };
  },

  componentDidMount() {
    LdapStore.loadSettings().done(settings => this.setState({ldapSettings: settings}));
  },

  _isLoading() {
    return !this.state.ldapSettings;
  },

  render() {
    if (this._isLoading()) {
      return <Spinner/>;
    }

    return (<span>
      Ldap form
    </span>);
  },
});

module.exports = LdapComponent;
