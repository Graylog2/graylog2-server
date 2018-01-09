import PropTypes from 'prop-types';
import React from 'react';
import { inject, observer } from 'mobx-react';
import { NavDropdown, MenuItem } from 'react-bootstrap';
import { LinkContainer } from 'react-router-bootstrap';

import Routes from 'routing/Routes';
import history from 'util/History';

const UserMenu = React.createClass({
  propTypes: {
    loginName: PropTypes.string.isRequired,
    fullName: PropTypes.string.isRequired,
    sessionId: PropTypes.string.isRequired,
    logout: PropTypes.func.isRequired,
  },
  onLogoutClicked() {
    this.props.logout(this.props.sessionId).then(() => {
      history.push(Routes.STARTPAGE);
    });
  },
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
  },
});

export default inject(context => ({
  sessionId: context.rootStore.sessionStore.sessionId,
  logout: sessionId => context.rootStore.sessionStore.logout(sessionId),
}))(observer(UserMenu));

