import React from 'react';
import Immutable from 'immutable';
import { LinkContainer } from 'react-router-bootstrap';
import { Row, Col, Input, Panel, Button } from 'react-bootstrap';
import naturalSort from 'javascript-natural-sort';

import { Spinner } from 'components/common';

import Routes from 'routing/Routes';

import ActionsProvider from 'injection/ActionsProvider';
const LdapGroupsActions = ActionsProvider.getActions('LdapGroups');

import StoreProvider from 'injection/StoreProvider';
const RolesStore = StoreProvider.getStore('Roles');
const LdapGroupsStore = StoreProvider.getStore('LdapGroups');

const LdapGroupsComponent = React.createClass({
  getInitialState() {
    return {
      groups: Immutable.Set.of(),
      roles: Immutable.Set.of(),
      mapping: Immutable.Map(),
      groupsErrorMessage: null,
    };
  },

  componentDidMount() {
    LdapGroupsActions.loadMapping.triggerPromise().then(mapping => this.setState({mapping: Immutable.Map(mapping)}));
    LdapGroupsActions.loadGroups.triggerPromise()
      .then(
        groups => this.setState({groups: Immutable.Set(groups)}),
        error => this.setState({groupsErrorMessage: error})
      );
    RolesStore.loadRoles().then(roles => this.setState({roles: Immutable.Set(roles)}));
  },

  _updateMapping(event) {
    const role = event.target.value;
    const group = event.target.getAttribute('data-group');
    if (role === '') {
      this.setState({mapping: this.state.mapping.delete(group)});
    } else {
      this.setState({mapping: this.state.mapping.set(group, role)});
    }
  },

  _saveMapping(event) {
    event.preventDefault();
    LdapGroupsActions.saveMapping(this.state.mapping.toJS());
  },

  _isLoading() {
    return !(this.state.mapping && this.state.groups && this.state.roles);
  },

  render() {
    if (this._isLoading()) {
      return <Spinner />;
    }

    if (this.state.groupsErrorMessage !== null) {
      return (
        <Panel header="Error: Unable to load LDAP groups" bsStyle="danger">
          The error message was:<br/>{this.state.groupsErrorMessage}
        </Panel>
      );
    }

    naturalSort.insensitive = true; // sigh

    const options = this.state.roles.sort(naturalSort).map(role => {
      return <option key={role.name} value={role.name}>{role.name}</option>;
    });

    const content = this.state.groups.sort(naturalSort).map(group => {
      return (
        <li key={group}>
          <Input label={group} data-group={group} type="select" value={this.state.mapping.get(group, '')}
                 onChange={this._updateMapping} labelClassName="col-sm-2" wrapperClassName="col-sm-5">
            <option value="">None</option>
            {options}
          </Input>
        </li>
      );
    });

    naturalSort.insensitive = false; // sigh 2

    if (content.size === 0) {
      return (
        <p>
          No LDAP/Active Directory groups found. Please verify that your{' '}
          <LinkContainer to={Routes.SYSTEM.LDAP.SETTINGS}><a>LDAP group mapping</a></LinkContainer>{' '}
          settings are correct.
        </p>
      );
    } else {
      return (
        <form className="form-horizontal" onSubmit={this._saveMapping}>
          <Row>
            <Col md={12}>
              <ul style={{padding: 0}}>{content}</ul>
            </Col>
            <Col md={10} mdPush={2}>
              <Button type="submit" bsStyle="success">Save</Button>&nbsp;
              <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.USERS.LIST}>
                <Button>Cancel</Button>
              </LinkContainer>
            </Col>
          </Row>
        </form>
      );
    }
  },
});

export default LdapGroupsComponent;
