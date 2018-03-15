import PropTypes from 'prop-types';
import React from 'react';
import { NavDropdown, MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import StoreProvider from 'injection/StoreProvider';
const SessionStore = StoreProvider.getStore('Session');

import ActionsProvider from 'injection/ActionsProvider';
const SessionActions = ActionsProvider.getActions('Session');

import Routes from 'routing/Routes';
import history from 'util/History';

class UserMenu extends React.Component {
  static propTypes = {
    loginName: PropTypes.string.isRequired,
    fullName: PropTypes.string.isRequired,
  };

  onLogoutClicked = () => {
    SessionActions.logout.triggerPromise(SessionStore.getSessionId()).then(() => {
      history.push(Routes.STARTPAGE);
    });
  };

  render() {
    return (
      <NavDropdown title={this.props.fullName} id="user-menu-dropdown">
        <LinkContainer to={Routes.SYSTEM.AUTHENTICATION.USERS.edit(encodeURIComponent(this.props.loginName))}>
          <MenuItem>Edit profile</MenuItem>
        </LinkContainer>
        <MenuItem divider />
        <MenuItem onSelect={this.onLogoutClicked}><i className="fa fa-sign-out" /> Log out</MenuItem>
      </NavDropdown>
    );
  }
}

export default UserMenu;

