/* global jsRoutes */
import React from 'react';
import { NavDropdown } from 'react-bootstrap';
import { MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import SessionActions from 'actions/sessions/SessionActions';
import SessionStore from 'stores/sessions/SessionStore';
import Routes from 'routing/Routes';

const UserMenu = React.createClass({
  onLogoutClicked() {
    SessionActions.logout(SessionStore.getSessionId());
  },
  render() {
    return (
      <NavDropdown navItem title={this.props.fullName} id="user-menu-dropdown">
        <LinkContainer to={Routes.USER_EDIT}>
          <MenuItem>Edit profile</MenuItem>
        </LinkContainer>
        <MenuItem divider />
        <MenuItem onSelect={this.onLogoutClicked}><i className="fa fa-sign-out"></i> Log out</MenuItem>
      </NavDropdown>
    );
  }
});

module.exports = UserMenu;
