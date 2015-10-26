import React from 'react';
import { NavDropdown, MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import SessionActions from 'actions/sessions/SessionActions';
import SessionStore from 'stores/sessions/SessionStore';
import Routes from 'routing/Routes';

const UserMenu = React.createClass({
  propTypes: {
    loginName: React.PropTypes.string.isRequired,
    fullName: React.PropTypes.string.isRequired,
  },
  onLogoutClicked() {
    SessionActions.logout(SessionStore.getSessionId());
  },
  render() {
    return (
      <NavDropdown navItem title={this.props.fullName} id="user-menu-dropdown">
        <LinkContainer to={Routes.SYSTEM.USERS.edit(this.props.loginName)}>
          <MenuItem>Edit profile</MenuItem>
        </LinkContainer>
        <MenuItem divider />
        <MenuItem onSelect={this.onLogoutClicked}><i className="fa fa-sign-out"/> Log out</MenuItem>
      </NavDropdown>
    );
  },
});

export default UserMenu;

