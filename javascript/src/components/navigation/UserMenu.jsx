/* global jsRoutes */
import React from 'react';
import { NavDropdown } from 'react-bootstrap';
import { MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';
import SessionActions from 'actions/sessions/SessionActions';
import SessionStore from 'stores/sessions/SessionStore';

const UserMenu = React.createClass({
  onLogoutClicked() {
    SessionActions.logout(SessionStore.getSessionId());
  },
  render() {
    return (
      <NavDropdown navItem title={this.props.fullName} id="user-menu-dropdown">
        <MenuItem href={jsRoutes.controllers.UsersController.editUserForm(this.props.loginName).url}>Edit profile</MenuItem>
        <MenuItem divider />
        <MenuItem onClick={this.onLogoutClicked}><i className="fa fa-sign-out"></i> Log out</MenuItem>
      </NavDropdown>
    );
  }
});

module.exports = UserMenu;
